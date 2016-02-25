(ns voke.core
  (:require [schema.core :as s]
            [goog.events :as events])
  (:import [goog.events KeyCodes])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [schema.core :as sm])
  (:use [cljs.core.async :only [chan <! >! put! timeout]]))

(sm/defschema Direction (s/enum :up :right :down :left))
(sm/defschema IntendedDirection #{Direction})

(sm/defschema Entity {:id                                s/Int
                      (s/maybe :position)                {:x s/Num
                                                          :y s/Num}
                      (s/maybe :collision-box)           {:width  s/Int
                                                          :height s/Int}
                      (s/maybe :render-info)             {:shape (s/enum :square)}
                      (s/maybe :human-controlled)        s/Bool
                      (s/maybe :indended-move-direction) IntendedDirection
                      (s/maybe :intended-fire-direction) IntendedDirection})

(def player {:id                      1
             :position                {:x 10
                                       :y 10}
             :collision-box           {:width 50 :height 50}
             :render-info             {:shape :square}
             :human-controlled        true
             :intended-move-direction #{}
             :intended-fire-direction #{}})

(sm/defschema GameState {:entities [Entity]})

; TODO :mode? :active-level?
(def game-state (atom {:entities [player]}))

(def move-key-mappings {KeyCodes.W :up
                        KeyCodes.A :left
                        KeyCodes.S :down
                        KeyCodes.D :right})
(def fire-key-mappings {KeyCodes.DOWN  :down
                        KeyCodes.LEFT  :left
                        KeyCodes.UP    :up
                        KeyCodes.RIGHT :right})
(def key-mappings (merge move-key-mappings fire-key-mappings))

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

(sm/defn update-player [state :- GameState & args]
  (apply update-in
         state
         (concat [:entities 0] (first args))
         (rest args)))

(defn handle-events [state event-chan]
  (listen-to-keyboard-inputs event-chan)

  (go-loop []
    (let [msg (<! event-chan)]
      (js/console.log (clj->js msg))
      (case (msg :type)
        :move-key-down (swap! state update-player [:intended-move-direction] conj (msg :direction))
        :move-key-up (swap! state update-player [:intended-move-direction] disj (msg :direction))
        :fire-key-down (swap! state update-player [:intended-fire-direction] conj (msg :direction))
        :fire-key-up (swap! state update-player [:intended-fire-direction] disj (msg :direction)))
      (recur))))

(sm/defn render-system
  [entities :- [Entity]]
  ; TODO  get a good canvas library via cljsjs, fabric or phaser seem to be top contenders, research
  (let [canvas (js/document.getElementById "screen")
        ctx (.getContext canvas "2d")]
    (.clearRect ctx
                0
                0
                (.-width canvas)
                (.-height canvas))
    (aset ctx "fillStyle" "rgb(50,50,50)")
    (doseq [entity entities]
      (.fillRect ctx
                 (-> entity :position :x)
                 (-> entity :position :y)
                 (-> entity :collision-box :width)
                 (-> entity :collision-box :height)))))

(def direction-value-mappings {:up    {:y -1}
                               :down  {:y 1}
                               :left  {:x -1}
                               :right {:x 1}})

(sm/defn move-system :- GameState
  [state :- GameState]
  (update-in state [:entities 0] (fn [entity]
                                   (loop [directions (entity :intended-move-direction)
                                          entity entity]
                                     (if (seq directions)
                                       (let [direction (first directions)
                                             [axis value] (first (direction-value-mappings direction))]
                                         (recur (rest directions)
                                                (update-in entity [:position axis] + value)))
                                       entity)))))

(sm/defn run-systems :- GameState
  [state :- GameState]
  (render-system (:entities state))
  (-> state
      move-system))

; useful in dev, so fighweel doesn't cause a jillion ticks of the system to happen at once
(defonce animation-frame-request-id (atom nil))

(defn ^:export main []
  (js/window.cancelAnimationFrame @animation-frame-request-id)

  (let [event-chan (chan)]
    (handle-events game-state event-chan))

  (js/window.requestAnimationFrame (fn process-frame [ts]
                                     (swap! game-state run-systems)
                                     (reset! animation-frame-request-id
                                             (js/window.requestAnimationFrame process-frame)))))
