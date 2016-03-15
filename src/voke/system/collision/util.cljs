(ns voke.system.collision.utils
  (:require [voke.events :refer [publish-event]]
            [voke.schemas :refer [Entity EntityID Vector2]]
            [voke.state :refer [update-entity!]])
  (:require-macros [schema.core :as sm]))

(sm/defn -track-entity
  [entity :- Entity]
  (js/Collision.addEntity (clj->js
                            (select-keys entity
                                         [:id :collision :shape]))))

(defn -update-entity-center
  [entity-id new-center]
  (js/Collision.updateEntity entity-id (clj->js new-center)))

(sm/defn -stop-tracking-entity
  [entity-id :- EntityID]
  (js/Collision.removeEntity entity-id))

(sm/defn apply-movement
  [entity :- Entity
   new-center
   new-velocity]
  "Fires events to notify the world that a particular entity should have a new center+velocity."
  (-update-entity-center (entity :id) new-center)
  (let [update-entity-fn (fn [entity]
                           (assert entity)
                           (-> entity
                               (update-in [:shape :center] merge new-center)
                               (update-in [:motion :velocity] merge new-velocity)))]
    (update-entity! (entity :id) :collision-system update-entity-fn)

    (publish-event {:event-type :movement
                    :entity     (update-entity-fn entity)})))

(sm/defn find-contacting-entities :- [Entity]
  ; XXX update docstring
  "Takes an Entity (one you're trying to move from one place to another) and a list of all of the
  Entities in the game. Returns another Entity if the space `entity` is trying to occupy is already filled,
  nil if the space `entity` is trying to occupy is empty."
  ; Critical path! Keep fast!
  [entity :- Entity
   new-center :- Vector2
   all-entities :- [Entity]]
  (let [contacting-entity-ids (js/Collision.findContactingEntityID (entity :id) (clj->js new-center))]
    (keep (fn [entity]
            (when (> (.indexOf contacting-entity-ids (entity :id))
                     -1)
              entity))
          all-entities)))
