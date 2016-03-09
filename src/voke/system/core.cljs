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

(sm/defn apply-update-entity-event [state :- GameState
                                    event :- Event]
  (if (contains? (state :entities)
                 (event :entity-id))
    (do
      ;(js/console.log (clj->js (event :origin)))
      ;(js/console.log (clj->js (get-in state [:entities (event :entity-id)])))
      ;(js/console.log (clj->js ((event :fn) (get-in state [:entities (event :entity-id)]))))
      (update-in state
                [:entities (event :entity-id)]
                (event :fn)))
    state))

(sm/defn make-system-runner
  "Returns a function from game-state -> game-state, which you can call to make a unit
  of time pass in the game-world."
  [game-state-atom
   player-entity-id]
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

    ; Handle :update-entity events.
    ; This is one of the main ways in which change is propagated through the game world.
    ; For instance, if voke.input notices the player's pressed a key on the keyboard,it updates the player's
    ; entity's state by publishing an :update-entity message, which the subscriber function below
    ; processes and applies. By the same token, the damage system listens for :contact events published
    ; by the collision system, and if it determines that a :contact event was between a body and a hostile
    ; bullet, it'll publish an :update-entity message which applies the relevant amount of damage to the
    ; relevant entity, etc.
    (subscribe-to-event :update-entity
                        (fn [event]
                          ; TODO consider batching this if it becomes a perf bottleneck
                          (swap! game-state-atom apply-update-entity-event event)
                          (js/console.log (clj->js (event :origin)))
                          (js/console.log (clj->js (get-in @game-state-atom [:entities])))
                          ))

    ; Handle :remove-entity events.
    (subscribe-to-event :remove-entity
                        (fn [event]
                          (swap! game-state-atom update-in [:entities] dissoc (event :entity-id))))

    ; And return a run-systems-every-tick function.
    (fn [state]
      ; feels like there must be a simpler way to express this loop statement, but i haven't found one
      ; TODO consider reduce, reduce always solves loops
      ;(js/console.log (clj->js (get-in @game-state-atom [:entities 0 :shape])))
      (loop [state state
             tick-functions (map system-to-tick-fn
                                 (filter :every-tick systems))]
        (if (seq tick-functions)
          (recur ((first tick-functions) state)
                 (rest tick-functions))
          state)))))
