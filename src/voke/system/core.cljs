(ns voke.system.core
  (:require [voke.events :refer [subscribe-to-event]]
            [voke.input :refer [handle-keyboard-events]]
            [voke.schemas :refer [GameState System]]
            [voke.system.attack :refer [attack-system]]
            [voke.system.collision :refer [collision-system]]
            [voke.system.movement :refer [move-system]]
            [voke.system.rendering :refer [render-system]])
  (:require-macros [schema.core :as sm]))

(sm/defn system-to-tick-fn
  "Takes a System map, returns a function of game-state -> game-state."
  [system :- System]
  (sm/fn [state :- GameState]
    (update-in state
               [:entities]
               merge
               (into {}
                     (map (juxt :id identity)
                          ((system :tick-fn) (vals (state :entities))))))))

(def game-systems [collision-system
                   attack-system
                   move-system
                   render-system])

(sm/defn make-system-runner
  "Returns a function from game-state -> game-state, which you can call to make a unit
  of time pass in the game-world."
  [player-entity-id]
  ; Set up systems' event handlers.
  (doseq [event-handler-map (flatten
                              (keep identity
                                    (map :event-handlers game-systems)))]
    (subscribe-to-event (event-handler-map :event-type)
                        #((event-handler-map :fn) %)))

  ; Listen to keyboard input.
  (handle-keyboard-events player-entity-id)

  ; Return a run-systems-every-tick function.
  (fn [state]
    (reduce (fn [state tick-function]
              (tick-function state))
            state
            (map system-to-tick-fn (filter :tick-fn game-systems)))))
