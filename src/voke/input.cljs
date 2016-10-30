(ns voke.input
  (:require [cljs.spec :as s]
            [clojure.set :refer [intersection difference subset?]]
            [clojure.string :refer [starts-with?]]
            [cljs.core.async :refer [chan <! put!]]
            [goog.events :as events]
            [voke.state :refer [update-entity! update-mode!]]
            [voke.util :refer [in?]])
  (:import [goog.events KeyCodes])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

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
          (when (= code KeyCodes.BACKSLASH)
            (update-mode! :visualization :keyboard-input))

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

(defn remove-conflicting-directions
  "Takes a seq of directions like #{:up :down :left} and returns a seq of directions
  where the pairs of conflicting directions - up+down, left+right - are stripped out if they're present."
  [directions]
  (reduce (fn [directions conflicting-pair]
            (if (= (intersection directions conflicting-pair)
                   conflicting-pair)
              (difference directions conflicting-pair)
              directions))
          directions
          [#{:up :down} #{:left :right}]))

(s/fdef remove-conflicting-directions
  :args (s/cat :directions (s/coll-of :input/intended-direction :kind set? :into #{}))
  :ret (s/coll-of :input/intended-direction)
  :fn #(subset? (set (% :ret))
                (-> % :args :directions)))

(defn human-controlled-entity-movement-directions
  [entity]
  (-> entity
      (get-in [:component/input :input/intended-move-direction])
      remove-conflicting-directions))

(s/fdef human-controlled-entity-movement-directions
  :args (s/and (s/cat :entity :entity/entity)
               #(contains? (% :entity) :component/input))
  :ret (s/coll-of :input/intended-direction))

(defn intended-directions->angle
  [intended-directions]
  (let [intended-direction-values (map direction-value-mappings intended-directions)]
    (Math/atan2 (apply + (map Math/sin intended-direction-values))
                (apply + (map Math/cos intended-direction-values)))))

(s/fdef intended-directions->angle
  :args (s/cat :intended-directions (s/coll-of :input/intended-direction))
  :ret :geometry/direction)

(defn update-move-direction
  [entity]
  (let [intended-directions (human-controlled-entity-movement-directions entity)]
    (if (seq intended-directions)
      (assoc-in entity
                [:component/motion :motion/direction]
                (intended-directions->angle intended-directions))
      (assoc-in entity
                [:component/motion :motion/direction]
                nil))))

(s/fdef update-move-direction
  :args (s/and (s/cat :entity :entity/entity)
               #(contains? (% :entity) :component/motion)
               #(contains? (% :entity) :component/input))
  :ret :entity/entity)


(defn update-fire-direction
  [entity]
  (let [intended-direction (last (get-in entity [:component/input :input/intended-fire-direction]))]
    (if intended-direction
      (assoc-in entity
                [:component/weapon :weapon/fire-direction]
                (intended-directions->angle [intended-direction]))
      (assoc-in entity [:component/weapon :weapon/fire-direction] nil))))

(s/fdef update-fire-direction
  :args (s/and (s/cat :entity :entity/entity)
               #(contains? (% :entity) :component/input)
               #(contains? (% :entity) :component/weapon))
  :ret :entity/entity)

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
          ; noop events are used by tests in order to coordinate with this go block; skip 'em
          (recur)

          (let [direction (msg :direction)
                update-entity-args (case (msg :type)
                                     :move-key-down [[:component/input :input/intended-move-direction] conj direction]
                                     :move-key-up [[:component/input :input/intended-move-direction] disj direction]
                                     :fire-key-down [[:component/input :input/intended-fire-direction]
                                                     (fn [fire-directions]
                                                       (if (in? fire-directions direction)
                                                         fire-directions
                                                         (conj fire-directions direction)))]
                                     :fire-key-up [[:component/input :input/intended-fire-direction]
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
