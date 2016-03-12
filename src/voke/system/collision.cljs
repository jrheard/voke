(ns voke.system.collision
  (:require [clojure.set :refer [difference]]
            [plumbing.core :refer [safe-get-in]]
            [schema.core :as s]
            [voke.events :refer [publish-event]]
            [voke.schemas :refer [Axis Entity EntityID Event Position Shape System]]
            [voke.state :refer [remove-entity! update-entity!]])
  (:require-macros [schema.core :as sm]))

; TODO - obstacles shouldn't be able to collide with each other
; will simplify world generation / wall placement

(def objects-by-entity-id (atom {}))

(defn to-arr [xs fun]
  (let [arr (array)]
    (doseq [x xs]
      (.push arr (fun x)))
    arr))

(sm/defn entity->js-obj
  [entity :- Entity]
  (let [collision (get entity :collision {})
        collides-with (collision :collides-with)
        collision-obj #js {:type          (name (collision :type))
                           :collides-with (if collides-with
                                            (to-arr collides-with name)
                                            nil)}
        shape (get entity :shape {})
        center (get shape :center {})
        center-obj #js {:x (center :x)
                        :y (center :y)}
        shape-obj #js {:center center-obj
                       :width  (shape :width)
                       :height (shape :height)}]

    #js {:id        (entity :id)
         :collision collision-obj
         :shape     shape-obj}))

(sm/defn -update-cached-js-objects
  [entities :- [Entity]]
  (let [objs @objects-by-entity-id
        seen (js-obj)]

    (doseq [entity entities]
      (let [entity-id (entity :id)]
        (aset seen entity-id true)

        (if (contains? objs entity-id)
          (let [obj (objs entity-id)
                center (aget (aget obj "shape") "center")]
            (aset center "x" (get-in entity [:shape :center :x]))
            (aset center "y" (get-in entity [:shape :center :y])))

          (swap! objects-by-entity-id assoc entity-id (clj->js entity)))))

    (doseq [entity-id (keys objs)]
      (when-not (aget seen entity-id)
        (swap! objects-by-entity-id dissoc entity-id)))))

(sm/defn find-entity-with-id :- Entity
  [entities :- [Entity]
   id :- EntityID]
  (some #(when (== (% :id) id) %)
        entities))

(sm/defn find-contacting-entity :- (s/maybe Entity)
  "Takes an Entity (one you're trying to move from one place to another) and a list of all of the
  Entities in the game. Returns another Entity if the space `entity` is trying to occupy is already filled,
  nil if the space `entity` is trying to occupy is empty."
  ; Critical path! Keep fast!
  [entity :- Entity
   all-entities :- [Entity]]
  (let [objs @objects-by-entity-id
        contacting-entity-id (js/Collision.findContactingEntityID (objs (entity :id))
                                                                  (to-arr (vals objs) identity))]
    (when contacting-entity-id
      (find-entity-with-id all-entities contacting-entity-id))))

(sm/defn find-closest-clear-spot :- (s/maybe s/Num)
  [event :- Event
   contacted-entity :- Entity]
  "Takes an :intended-movement event (for entity A) and the Entity that occupies the position that entity A
  is trying to move to (entity B). Finds the closest x- or y-value (depending on the value of `(event :axis)`)
  that entity A can occupy without contacting entity B and returns it if entity A fits there, or returns nil
  if no open spot exists."
  ; TODO - only supports rectangles
  (let [shape1 ((event :entity) :shape)
        shape2 (contacted-entity :shape)
        arithmetic-fn (if (pos? (event :new-velocity)) - +)
        field (if (= (event :axis) :x) :width :height)
        axis-value-to-try (arithmetic-fn (get-in shape2 [:center (event :axis)])
                                         (/ (shape2 field) 2)
                                         (/ (shape1 field) 2)
                                         0.01)]
    (when-not (find-contacting-entity (assoc-in (event :entity)
                                                [:shape :center (event :axis)]
                                                axis-value-to-try)
                                      (event :all-entities))
      axis-value-to-try)))

(sm/defn apply-movement
  [entity :- Entity
   axis :- Axis
   new-position :- s/Num
   new-velocity :- s/Num]
  "Fires events to notify the world that a particular entity should have a new position+velocity."
  (let [update-entity-fn (fn [entity]
                           (assert entity)
                           (-> entity
                               (assoc-in [:shape :center axis] new-position)
                               (assoc-in [:motion :velocity axis] new-velocity)))]
    (update-entity! (entity :id) :collision-system update-entity-fn)

    (let [obj (@objects-by-entity-id (entity :id))
          center (-> obj
                     (aget "shape")
                     (aget "center"))]
      (aset center (name axis) new-position))

    (publish-event {:event-type :movement
                    :entity     (update-entity-fn entity)})))

(sm/defn handle-contact
  [event :- Event
   contacted-entity :- Entity]
  (if (get-in event [:entity :collision :destroyed-on-contact])
    ; This entity should be destroyed on contact, and we're handling a contact. Destroy it!
    (remove-entity! (safe-get-in event [:entity :id]) :collision-system)

    ; This entity doesn't need to be destroyed on contact. Let it live.
    (if-let [closest-clear-spot (find-closest-clear-spot event contacted-entity)]
      ; Great, we found a clear spot nearby! Move there and stand still.
      (apply-movement (event :entity)
                      (event :axis)
                      closest-clear-spot
                      0)

      ; Couldn't find a clear spot; slow the entity down, it can try moving again next tick.
      (update-entity! (safe-get-in event [:entity :id])
                      :collision-system
                      (fn [old-entity]
                        (update-in old-entity
                                   [:motion :velocity (event :axis)]
                                   #(* % 0.7))))))

  (publish-event {:event-type :contact
                  :entities   [(event :entity) contacted-entity]}))

(sm/defn handle-intended-movement
  [event :- Event]
  (let [entity (event :entity)
        moved-entity (assoc-in entity
                               [:shape :center (event :axis)]
                               (event :new-position))]

    (if-let [contacted-entity (find-contacting-entity moved-entity (event :all-entities))]
      (handle-contact event contacted-entity)
      (apply-movement entity
                      (event :axis)
                      (event :new-position)
                      (event :new-velocity)))))

;; System definition

(sm/def collision-system :- System
  {:initialize     (fn [game-state]
                     (doseq [entity (vals (game-state :entities))]
                       (swap! objects-by-entity-id assoc (entity :id) (clj->js entity))))

   :event-handlers [{:event-type :entity-added
                     :fn         (fn [event]
                                   (swap! objects-by-entity-id
                                          assoc
                                          (get-in event [:entity :id])
                                          (clj->js (event :entity))))}
                    {:event-type :entity-removed
                     :fn         (fn [event]
                                   (swap! objects-by-entity-id
                                          dissoc
                                          (event :entity-id)))}
                    {:event-type :intended-movement
                     :fn         handle-intended-movement}]})
