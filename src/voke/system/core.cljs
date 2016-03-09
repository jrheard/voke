(ns voke.system.core
  (:require [cljs.core.async :refer [chan <! put!]]
            [schema.core :as s]
            [voke.events :refer [subscribe-to-event]]
            [voke.input :refer [handle-keyboard-events]]
            [voke.schemas :refer [Entity Event GameState System]]
            [voke.system.attack :refer [attack-system]]
            [voke.system.collision :refer [collision-system]]
            [voke.system.movement :refer [move-system]]
            [voke.system.rendering :refer [render-system]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [schema.core :as sm]))

(sm/defn has-relevant-fields? :- s/Bool
  [entity :- Entity
   fields]
  (every?
    (fn [field]
      (let [value (entity field)]
        (if (coll? value)
          (seq value)
          value)))
    fields))

(sm/defn system-to-tick-fn
  "Takes a System map, returns a function of game-state -> game-state.
  Basically takes a System map and turns it into something you can run every tick.

  The job of a System-based tick function (found in a System's [:every-tick :fn]) is to take
  a list of 'relevant' entities (defined by the System's [:every-tick :reads] set) as input,
  and return a list of 'processed' entities as output. For instance, the movement system takes a list of
  moving entities and returns a list of entities that have been nudged a bit in the direction they're heading."
  [system :- System]
  (sm/fn [state :- GameState]
    (let [tick-specification (system :every-tick)
          filter-fn (if (contains? tick-specification :reads)
                      identity
                      #(has-relevant-fields? % (tick-specification :reads)))
          relevant-entities (filter filter-fn (vals (state :entities)))
          processed-entities ((tick-specification :fn) relevant-entities)]

      (update-in state
                 [:entities]
                 merge
                 (into {}
                       (map (juxt :id identity)
                            processed-entities))))))

(sm/defn make-system-runner
  "Returns a function from game-state -> game-state, which you can call to make a unit
  of time pass in the game-world."
  [player-entity-id]
  (let [systems [collision-system
                 attack-system
                 move-system
                 render-system]
        event-handlers (flatten
                         (keep identity
                               (map :event-handlers systems)))]

    ; Set up systems' event handlers.
    (doseq [handler-map event-handlers]
      (subscribe-to-event (handler-map :event-type)
                          #((handler-map :fn) %)))

    ; Listen to keyboard input.
    (handle-keyboard-events player-entity-id)

    ; Return a run-systems-every-tick function.
    (fn [state]
      ; feels like there must be a simpler way to express this loop statement, but i haven't found one
      ; TODO consider reduce, reduce always solves loops
      (loop [state state
             tick-functions (map system-to-tick-fn
                                 (filter :every-tick systems))]
        (if (seq tick-functions)
          (recur ((first tick-functions) state)
                 (rest tick-functions))
          state)))))
