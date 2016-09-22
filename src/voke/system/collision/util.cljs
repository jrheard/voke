(ns voke.system.collision.util
  (:require [voke.events :refer [publish-event]]
            [voke.schemas :refer [Entity EntityID Vector2]]
            [voke.state :refer [update-entity!]]
            [voke.system.collision.state :refer [dead-entities]])
  (:require-macros [schema.core :as sm]))

(defn vector2->js [vector2]
  #js {:x (vector2 :x)
       :y (vector2 :y)})

(sm/defn -track-entity
  [entity :- Entity]
  (-> entity
      (select-keys [:id :collision :shape])
      clj->js
      js/Collision.addEntity))

(defn -update-entity-center
  [entity-id new-center]
  (js/Collision.updateEntity entity-id (vector2->js new-center)))

(sm/defn -stop-tracking-entity
  [entity-id :- EntityID]
  (js/Collision.removeEntity entity-id))

(sm/defn apply-movement
  "Fires events to notify the world that a particular entity should have a new center+velocity."
  [entity :- Entity
   new-center :- Vector2
   new-velocity :- Vector2]
  ; Queue an update to this entity's center and velocity; these updates get run in a single batch at the
  ; end of this tick, so that all entities in cljs-land have their centers+velocities updated at once.
  (update-entity! (entity :id)
                  :collision-system
                  (fn [entity]
                    (assert entity)
                    (-> entity
                        (assoc-in [:shape :center] new-center)
                        (assoc-in [:motion :velocity] new-velocity))))

  ; *Immediately* register the entity's new center with the JS-land collision system, though,
  ; so that it always has the most up-to-date view possible of where each entity is.
  ;
  ; In order to be correct, the voke collision system needs to maintain this invariant:
  ; at all times, no two entities are occuping the same space.
  ; By updating the JS-land collision system multiple times per tick, we maintain this invariant.
  ; If we did not do this, two entities moving toward each other could both end up occuping
  ; the same space.
  (-update-entity-center (entity :id) new-center)

  (publish-event {:type       :movement
                  :entity-id  (entity :id)
                  :new-center new-center}))

(sm/defn remove-entity!
  "Wrapper around voke.state/remove-entity! so that we can keep track of which entities have been destroyed
  by us during this tick."
  [entity :- Entity]
  ; The collision system should only be killing :destroyed-on-contact entities.
  (assert (get-in entity [:collision :destroyed-on-contact]))

  (when-not (contains? @dead-entities (entity :id))
    (swap! dead-entities conj (entity :id))
    (voke.state/remove-entity! (entity :id) :collision-system)))

(sm/defn get-updated-entity-center :- Entity
  "Returns the given entity, with its most up-to-date center value spliced in.
  See `apply-movement` for an explanation of why the js-land collision system has more up-to-date center values."
  [entity :- Entity]
  (assoc-in entity
            [:shape :center]
            (js->clj (js/Collision.getEntityCenter (entity :id))
                     :keywordize-keys true)))

(sm/defn find-contacting-entities :- [Entity]
  "Takes an Entity (one you're trying to move from one place to another) and a list of all of the
  Entities in the game. Returns a (possibly empty) list of all of the other entities that `entity` would collide
  with if it were to move to `new-center`."
  ; Critical path! Keep fast!
  [entity :- Entity
   new-center :- Vector2
   all-entities :- [Entity]]
  (let [contacting-entity-ids (js/Collision.findContactingEntityIDs (entity :id) (vector2->js new-center))]
    (if (> (.-length contacting-entity-ids) 0)
      (let [entity-ids (set (js->clj contacting-entity-ids))]
        (keep (fn [entity]
                (when (contains? entity-ids (entity :id))
                  (get-updated-entity-center entity)))
              all-entities))
      [])))
