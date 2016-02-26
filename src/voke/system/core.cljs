(ns voke.system.core
  (:require [schema.core :as s]
            [voke.events :refer [make-pub subscribe-to-event]]
            [voke.schemas :refer [Entity EntityField GameState System]]
            [voke.system.movement :refer [move-system]]
            [voke.system.rendering :refer [render-system a-rendering-event-handler]])
  (:require-macros [schema.core :as sm]))

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

(sm/defn system-to-tick-fn [system :- System]
  (sm/fn [state :- GameState
          publish-chan]
    (let [tick-specification (system :every-tick)
          ; TODO - if the system has no :reads key, *all* entities are relevant.
          ; i feel like there was a case where this was important. maybe not. if there's not a case
          ; like that then delete this and make :reads required in the schema again
          relevant-entities (filter #(has-relevant-fields? % (tick-specification :reads))
                                    (vals (state :entities)))
          processed-entities ((tick-specification :fn) relevant-entities publish-chan)]

      (update-in state
                 [:entities]
                 merge
                 (into {}
                       (map (juxt :id identity)
                            processed-entities))))))


(sm/defn make-system-runner []
  (let [{:keys [publish-chan publication]} (make-pub)
        systems [move-system
                 render-system]
        event-handlers (flatten
                         (keep identity
                               (map :event-handlers systems)))]

    ; Set up event handlers...
    (doseq [handler-map event-handlers]
      (subscribe-to-event publication
                          (handler-map :event-type)
                          (handler-map :fn)))

    ; And return a run-systems-every-tick function.
    (fn [state]
      ; feels like there must be a simpler way to express this loop statement, but i haven't found one
      (loop [state state
             tick-functions (map system-to-tick-fn systems)]
        (if (seq tick-functions)
          (recur ((first tick-functions) state publish-chan)
                 (rest tick-functions))
          state)))))

(def run-systems (make-system-runner))
