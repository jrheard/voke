(ns voke.system.core
  (:require [cljs.core.async :refer [chan <! put!]]
            [schema.core :as s]
            [voke.events :refer [make-pub subscribe-to-event]]
            [voke.input :refer [handle-keyboard-events]]
            [voke.schemas :refer [Entity EntityField GameState System]]
            [voke.system.movement :refer [move-system]]
            [voke.system.rendering :refer [render-system]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [schema.core :as sm]))

; TODO use safe-get-in

(sm/defn has-relevant-fields? :- s/Bool
  [entity :- Entity
   fields :- #{EntityField}]
  (every?
    (fn [field]
      (let [value (entity field)]
        (if (coll? value)
          (seq value)
          value)))
    fields))

(sm/defn system-to-tick-fn
  "Takes a System map, returns a function of [game-state publish-chan] -> game-state.
  Basically takes a System map and turns it into something you can run every tick."
  [system :- System]
  (sm/fn [state :- GameState
          publish-chan]
    (let [tick-specification (system :every-tick)
          ; TODO - if the system has no :reads key, *all* entities are relevant.
          ; i feel like there was a case where this was important. maybe not. if there's not a case
          ; like that then delete this comment and make :reads required in the schema again
          relevant-entities (filter #(has-relevant-fields? % (tick-specification :reads))
                                    (vals (state :entities)))
          processed-entities ((tick-specification :fn) relevant-entities publish-chan)]

      (update-in state
                 [:entities]
                 merge
                 (into {}
                       (map (juxt :id identity)
                            processed-entities))))))

; ok whose job is it to make the publisher/publish-chan
; system.core or voke.core?
; we need 'em here
(sm/defn handle-update-entity-events [publish-chan ])

(sm/defn apply-update-entity-event [state :- GameState
                                    event ; TODO schematize
                                    ]
  (let [{:keys [entity-id args]} event]
    (apply update-in
           state
           (concat [:entities entity-id] (first args))
           (rest args))))

(sm/defn make-system-runner
  "Returns a function from game-state -> game-state, which you can call to make a unit
  of time pass in the game-world."
  [game-state-atom
   player-entity-id]
  (let [systems [move-system
                 render-system]
        {:keys [publish-chan publication]} (make-pub)
        event-handlers (flatten
                         (keep identity
                               (map :event-handlers systems)))]

    ; Set up event handlers...
    (doseq [handler-map event-handlers]
      (subscribe-to-event publication
                          (handler-map :event-type)
                          ; TODO should event handlers get a publish-chan, too? they don't currently
                          (handler-map :fn)))

    ; Listen to keyboard input...
    (handle-keyboard-events publish-chan player-entity-id)

    ; Handle :update-entity events...
    (subscribe-to-event publication
                        :update-entity
                        (fn [event]
                          (swap! game-state-atom apply-update-entity-event event)))

    ; And return a run-systems-every-tick function.
    (fn [state]
      ; feels like there must be a simpler way to express this loop statement, but i haven't found one
      (loop [state state
             ; TODO what about systems that don't have an every-tick function (eg damage)?
             tick-functions (map system-to-tick-fn systems)]
        (if (seq tick-functions)
          (recur ((first tick-functions) state publish-chan)
                 (rest tick-functions))
          state)))))
