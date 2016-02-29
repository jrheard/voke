(ns voke.system.collision
  (:require [schema.core :as s]
            [voke.events :refer [publish-event]]
            [voke.schemas :refer [Entity Event]])
  (:require-macros [schema.core :as sm]))

(sm/defn find-contacting-entity :- (s/maybe Entity)
  "TODO: DOCUMENT SEMANTICS"
  [entity :- Entity
   all-entities :- [Entity]]
  nil
  )

(sm/defn handle-intended-movement
  [event :- Event
   publish-chan]
  (let [entity (event :moved-entity)]
    (if-let [contacted-entity (find-contacting-entity entity (event :all-entities))]
      ; New position wasn't clear; contact!
      (publish-event publish-chan {:event-type :contact
                                   :entities   [entity contacted-entity]})

      ; Position was clear, go ahead and apply the intended movement.
      (do
        (publish-event publish-chan {:event-type :update-entity
                                     :origin     :collision-system
                                     :entity-id  (entity :id)
                                     :args       [[:shape] merge (entity :shape)]})
        (publish-event publish-chan {:event-type :movement
                                     :entity     entity})))))

(def collision-system
  {:event-handlers [{:event-type :intended-movement
                     :fn         handle-intended-movement}]})
