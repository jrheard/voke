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

(def objects-by-entity-id (js-obj))

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
  (let [contacting-entity-id (js/Collision.findContactingEntityID (entity->js-obj entity)
                                                                  objects-by-entity-id)]
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

    (let [obj (aget objects-by-entity-id (entity :id))
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

; TODO have this just be a plain old function (not an event handler) called attempt-to-move
(defn handle-intended-movement
  [{:keys [entity axis new-position new-velocity all-entities] :as event}]
  (let [moved-entity (assoc-in entity
                               [:shape :center axis]
                               new-position)]

    (if-let [contacted-entity (find-contacting-entity moved-entity all-entities)]
      (handle-contact event contacted-entity)
      (apply-movement entity axis new-position new-velocity))))

;; System definition

(sm/def collision-system :- System
  {:initialize     (fn [game-state]
                     (doseq [entity (vals (game-state :entities))]
                       (aset objects-by-entity-id
                             (entity :id)
                             (entity->js-obj entity))))

   :event-handlers [{:event-type :entity-added
                     :fn         (fn [event]
                                   (aset objects-by-entity-id
                                         (get-in event [:entity :id])
                                         (entity->js-obj (event :entity))))}
                    {:event-type :entity-removed
                     :fn         (fn [event]
                                   (js-delete objects-by-entity-id (event :entity-id)))}
                    {:event-type :intended-movement
                     :fn         handle-intended-movement}]})
