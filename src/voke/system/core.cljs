(ns voke.system.core
  (:require [cljs.spec :as s]
            [voke.events :refer [subscribe-to-event]]
            [voke.input :refer [handle-keyboard-events]]
            [voke.system.ai.system :refer [ai-system]]
            [voke.system.attack :refer [attack-system]]
            [voke.system.collision.system :refer [collision-system]]
            [voke.system.movement :refer [move-system]]
            [voke.system.rendering :refer [render-system]]))

;; Specs

(s/def :system/tick-fn fn?)
(s/def :system/initialize fn?)
(s/def :system/event-handler-fn fn?)
(s/def :system/event-handler (s/keys :req [:event/type :system/event-handler-fn]))
(s/def :system/event-handlers (s/coll-of :system/event-handlers))

(s/def :system/system (s/keys :opt [:system/tick-fn :system/initialize :system/event-handlers]))

;; Private

(defn system-to-tick-fn
  "Takes a System map, returns a function of game-state -> game-state."
  [system]
  (fn [state]
    (update-in state
               [:entities]
               merge
               ; XXXX assert that the line below contains only entities that already exist in state :entities keys
               (into {}
                     (map (juxt :id identity)
                          ((system :tick-fn) (vals (state :entities))))))))

(s/fdef system-to-tick-fn
  :args (s/cat :system :system/system)
  :ret fn?)

; smell: collision system is listed first so that its tick function can reset its internal state atoms
; before anything else can happen in each frame.
; should systems have a :before-tick function that serves this purpose?
(def game-systems [collision-system
                   move-system
                   attack-system
                   ai-system
                   render-system])

(def tick-functions
  (map system-to-tick-fn
       (filter :tick-fn game-systems)))

;; Public

(defn initialize-systems!
  [game-state player-entity-id]

  ; Run systems' initalize functions.
  (doseq [initialize-fn (keep identity
                              (map :initialize game-systems))]
    (initialize-fn game-state))

  ; Set up systems' event handlers.
  (doseq [event-handler-map (flatten
                              (keep identity
                                    (map :event-handlers game-systems)))]
    (subscribe-to-event (event-handler-map :event-type)
                        (event-handler-map :fn)))

  ; Listen to keyboard input.
  (handle-keyboard-events player-entity-id))

(defn process-a-tick
  "A function from game-state -> game-state, which you can call to make a unit
  of time pass in the game-world."
  [state]
  (reduce (fn [state tick-function]
            (tick-function state))
          state
          tick-functions))

(s/fdef process-a-tick
  :args (s/cat :state :game-state/game-state)
  :ret :game-state/game-state)
