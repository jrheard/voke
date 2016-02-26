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

; TODO
; turn each system map into a function
; it should take in entities with all the relevant keys
; and call its function on each matching entity [TODO or all the entities at once, can't decide]
; yeah i want it to do all the entities at once
; ok so i guess we first convert state's :entities into a map keyed by entity id
; and then we make a new map and merge the two

(sm/defn system-to-tick-fn [system :- System]
  (sm/fn [state :- GameState
          publish-chan]
    (let [tick-specification (system :every-tick)
          relevant-entities (filter #(has-relevant-fields? % (tick-specification :reads))
                                    (vals (state :entities)))
          processed-entities ((tick-specification :fn) relevant-entities publish-chan)]

      (update-in state
                 [:entities]
                 merge
                 ; TODO reimplement next expression more simply
                 (apply hash-map
                        (flatten
                          (for [entity processed-entities]
                            [(entity :id) entity])))))))


(sm/defn make-system-runner []
  (let [{:keys [publish-chan publication]} (make-pub)]
    (subscribe-to-event publication :movement a-rendering-event-handler)

    ; hm - how to express this situation? i have a dynamic list of things that i want to thread state through
    ; maybe "apply comp"?
    ; oh fuck it just do a loop/recur for now and figure out a prettier way later
    (fn [state]
      (loop [state state
             tick-functions (map system-to-tick-fn [move-system render-system])]
        (if (seq tick-functions)

          (recur ((first tick-functions) state publish-chan)
                 (rest tick-functions))
          state)))))

(def run-systems (make-system-runner))
