(ns voke.input
  (:require [clojure.set :refer [intersection difference]]
            [clojure.string :refer [starts-with?]]
            [cljs.core.async :refer [chan <! put!]]
            [goog.events :as events]
            [plumbing.core :refer [safe-get-in]]
            [voke.schemas :refer [Direction Entity IntendedDirection]]
            [voke.state :refer [update-entity!]]
            [voke.util :refer [in?]])
  (:import [goog.events KeyCodes])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [schema.core :as sm]))

(def move-key-mappings {KeyCodes.W     :up
                        KeyCodes.A     :left
                        KeyCodes.S     :down
                        KeyCodes.D     :right
                        ; carmen asked me to add dvorak support
                        KeyCodes.COMMA :up
                        KeyCodes.O     :down
                        KeyCodes.E     :right})
(def fire-key-mappings {KeyCodes.DOWN  :down
                        KeyCodes.LEFT  :left
                        KeyCodes.UP    :up
                        KeyCodes.RIGHT :right})
(def key-mappings (merge move-key-mappings fire-key-mappings))

;; Browser-level input handling

(defn listen-to-keyboard-inputs [event-chan]
  (events/removeAll (.-body js/document))

  (doseq [[goog-event-type event-type-suffix] [[(.-KEYDOWN events/EventType) "down"]
                                               [(.-KEYUP events/EventType) "up"]]]

    (events/listen
      (.-body js/document)
      goog-event-type
      (fn [event]
        (let [code (.-keyCode event)]
          (when (contains? key-mappings code)
            (.preventDefault event)
            (let [event-type (cond
                               (contains? move-key-mappings code) (keyword (str "move-key-" event-type-suffix))
                               (contains? fire-key-mappings code) (keyword (str "fire-key-" event-type-suffix)))]
              (put! event-chan {:type      event-type
                                :direction (key-mappings code)}))))))))

;; Game-engine-level logic

(def direction-value-mappings {:down  (/ Math/PI 2)
                               :up    (- (/ Math/PI 2))
                               :left  Math/PI
                               :right 0})

(sm/defn remove-conflicting-directions :- [IntendedDirection]
  "Takes a seq of directions like #{:up :down :left} and returns a seq of directions
  where the pairs of conflicting directions - up+down, left+right - are stripped out if they're present."
  [directions :- [IntendedDirection]]
  (reduce (fn [directions conflicting-pair]
            (if (= (intersection directions conflicting-pair)
                   conflicting-pair)
              (difference directions conflicting-pair)
              directions))
          directions
          [#{:up :down} #{:left :right}]))

(sm/defn human-controlled-entity-movement-directions
  [entity :- Entity]
  (-> entity
      (safe-get-in [:input :intended-move-direction])
      remove-conflicting-directions))

(sm/defn intended-directions->angle :- Direction
  [intended-directions :- [IntendedDirection]]
  (let [intended-direction-values (map direction-value-mappings intended-directions)]
    (Math/atan2 (apply + (map Math/sin intended-direction-values))
                (apply + (map Math/cos intended-direction-values)))))

(sm/defn update-move-direction :- Entity
  [entity :- Entity]
  (let [intended-directions (human-controlled-entity-movement-directions entity)]
    (if (seq intended-directions)
      (assoc-in entity
                [:motion :direction]
                (intended-directions->angle intended-directions))
      (assoc-in entity
                [:motion :direction]
                nil))))

(sm/defn update-fire-direction :- Entity
  [entity :- Entity]
  (let [intended-direction (last (get-in entity [:input :intended-fire-direction]))]
    (if intended-direction
      (assoc-in entity
                [:weapon :fire-direction]
                (intended-directions->angle [intended-direction]))
      (assoc-in entity [:weapon :fire-direction] nil))))

;;; Public

(defn handle-keyboard-events
  "Listens to keyboard events, and queues modifications to the `player-id` entity
  whenever a move key or fire key is pressed/raised."
  [player-id]
  (let [event-chan (chan)]
    (listen-to-keyboard-inputs event-chan)

    (go-loop []
      (let [msg (<! event-chan)]
        (if (= (msg :type) :noop)
          (recur)

          (let [direction (msg :direction)
                update-entity-args (case (msg :type)
                                     :move-key-down [[:input :intended-move-direction] conj direction]
                                     :move-key-up [[:input :intended-move-direction] disj direction]
                                     :fire-key-down [[:input :intended-fire-direction]
                                                     (fn [fire-directions]
                                                       (if (in? fire-directions direction)
                                                         fire-directions
                                                         (conj fire-directions direction)))]
                                     :fire-key-up [[:input :intended-fire-direction]
                                                   (fn [fire-directions]
                                                     (filterv #(not= direction %) fire-directions))])]

            (update-entity! player-id
                            :keyboard-input
                            (fn [entity]
                              (let [entity (apply update-in entity update-entity-args)]
                                (if (starts-with? (name (msg :type)) "fire")
                                  (update-fire-direction entity)
                                  (update-move-direction entity)))))))
        (recur)))))
