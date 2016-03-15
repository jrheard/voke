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
  "Takes an Entity (one you're trying to move from one place to another) and a list of all of the
  Entities in the game. Returns a (possibly empty) list of all of the other entities that `entity` would collide
  with if it were to move to `new-center`."
  ; Critical path! Keep fast!
  [entity :- Entity
   new-center :- Vector2
   all-entities :- [Entity]]
  (let [contacting-entity-ids (js/Collision.findContactingEntityID (entity :id) (clj->js new-center))]
    (if (> (.-length contacting-entity-ids) 0)
      (let [entity-ids (set (js->clj contacting-entity-ids))]
        (keep (fn [entity]
                (when (contains? entity-ids (entity :id))
                  entity))
              all-entities))
      [])))
