(ns voke.system.collision.util
  (:require [cljs.spec :as s]
            [voke.events :refer [publish-event]]
            [voke.state :refer [update-entity!]]
            [voke.system.collision.state :refer [dead-entities]]))

(defn vector2->js [vector2]
  #js {:x (vector2 :geometry/x)
       :y (vector2 :geometry/y)})

(defn -track-entity
  [entity]
  (-> entity
      (select-keys [:entity/id :component/collision :component/shape])
      clj->js
      js/Collision.addEntity))

(s/fdef -track-entity
  :args (s/cat :entity :entity/entity))

(defn -update-entity-center
  [entity-id new-center]
  (js/Collision.updateEntity entity-id (vector2->js new-center)))

(defn -stop-tracking-entity
  [entity-id]
  (js/Collision.removeEntity entity-id))

(defn apply-movement
  "Fires events to notify the world that a particular entity should have a new center+velocity."
  [entity new-center new-velocity]
  ; Queue an update to this entity's center and velocity; these updates get run in a single batch at the
  ; end of this tick, so that all entities in cljs-land have their centers+velocities updated at once.
  (update-entity! (entity :entity/id)
                  :collision-system
                  (fn [entity]
                    (assert entity)
                    (-> entity
                        (assoc-in [:component/shape :shape/center] new-center)
                        (assoc-in [:component/motion :motion/velocity] new-velocity))))

  ; *Immediately* register the entity's new center with the JS-land collision system, though,
  ; so that it always has the most up-to-date view possible of where each entity is.
  ;
  ; In order to be correct, the voke collision system needs to maintain this invariant:
  ; at all times, no two entities are occuping the same space.
  ; By updating the JS-land collision system multiple times per tick, we maintain this invariant.
  ; If we did not do this, two entities moving toward each other could both end up occuping
  ; the same space.
  (-update-entity-center (entity :entity/id) new-center)

  (publish-event {:event/type :movement
                  :entity-id  (entity :entity/id)
                  :new-center new-center}))

(s/fdef apply-movement
  :args (s/cat :entity :entity/entity
               :new-center :geometry/vector2
               :new-velocity :geometry/vector2))

(defn remove-entity!
  "Wrapper around voke.state/remove-entity! so that we can keep track of which entities have been destroyed
  by us during this tick."
  [entity]
  ; The collision system should only be killing :destroyed-on-contact entities.
  (assert (get-in entity [:collision :destroyed-on-contact]))

  (when-not (contains? @dead-entities (entity :entity/id))
    (swap! dead-entities conj (entity :entity/id))
    (voke.state/remove-entity! (entity :entity/id) :collision-system)))

(s/fdef remove-entity!
  :args (s/and (s/cat :entity :entity/entity)
               #(-> % :entity :collision :destroyed-on-contact true?)))

(defn get-updated-entity-center
  "Returns the given entity, with its most up-to-date center value spliced in.
  See `apply-movement` for an explanation of why the js-land collision system has more up-to-date center values."
  [entity]
  (assoc-in entity
            [:component/shape :shape/center]
            (js->clj (js/Collision.getEntityCenter (entity :entity/id))
                     :keywordize-keys true)))

(s/fdef get-updated-entity-center
  :args (s/cat :entity :entity/entity)
  :ret :entity/entity)

(defn find-contacting-entities
  "Takes an Entity (one you're trying to move from one place to another) and a list of all of the
  Entities in the game. Returns a (possibly empty) list of all of the other entities that `entity` would collide
  with if it were to move to `new-center`."
  ; Critical path! Keep fast!
  [entity new-center all-entities]
  (let [contacting-entity-ids (js/Collision.findContactingEntityIDs (entity :entity/id) (vector2->js new-center))]
    (if (> (.-length contacting-entity-ids) 0)
      (let [entity-ids (set (js->clj contacting-entity-ids))]
        (keep (fn [entity]
                (when (contains? entity-ids (entity :entity/id))
                  (get-updated-entity-center entity)))
              all-entities))
      [])))

(s/fdef find-contacting-entities
  :args (s/cat :entity :entity/entity
               :new-center :geometry/vector2
               :all-entities (s/coll-of :entity/entity))
  :ret (s/coll-of :entity/entity))
