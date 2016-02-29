(ns voke.system.collision
  (:require [schema.core :as s]
            [voke.events :refer [publish-event]]
            [voke.schemas :refer [Entity Event]])
  (:require-macros [schema.core :as sm]))

(sm/defn is-entity-position-clear :- s/Bool
  [entity :- Entity
   all-entities :- [Entity]]

  )
; TODO we'll need to see the entire set of entities

(sm/defn handle-intended-movement
  [event :- Event
   publish-chan]
  (js/console.log (clj->js event))

  ; check to see if the new position is unoccupied
  ; if it is, fire an :update-entity event (see voke.input) to apply the new position
  ; if it *isn't*, fire a :contact event
  )

(def collision-system
  {:event-handlers [{:event-type :intended-movement
                     :fn         handle-intended-movement}]})
