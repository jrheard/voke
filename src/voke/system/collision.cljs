(ns voke.system.collision
  (:require [plumbing.core :refer [safe-get-in]]
            [schema.core :as s]
            [voke.events :refer [publish-event]]
            [voke.schemas :refer [Axis Entity Event System]]
            [voke.state :refer [remove-entity! update-entity!]])
  (:require-macros [schema.core :as sm]))

(defn left-edge-x [rect] (- (rect :x)
                            (/ (rect :width) 2)))
(defn right-edge-x [rect] (+ (rect :x)
                             (/ (rect :width) 2)))
(defn top-edge-y [rect] (- (rect :y)
                           (/ (rect :height) 2)))
(defn bottom-edge-y [rect] (+ (rect :y)
                              (/ (rect :height) 2)))

; TODO - obstacles shouldn't be able to collide with each other
; will simplify world generation / wall placement

(sm/defn shapes-collide? :- s/Bool
  [shape1 shape2]
  ; right now everything's just aabbs
  ; when that changes, this function will need to get smarter
  (not-any? identity
            [(< (bottom-edge-y shape1) (top-edge-y shape2))
             (> (top-edge-y shape1) (bottom-edge-y shape2))
             (> (left-edge-x shape1) (right-edge-x shape2))
             (< (right-edge-x shape1) (left-edge-x shape2))]))

(sm/defn entities-can-collide? :- s/Bool
  "Returns true if two entities are *able* to collide with one another, false otherwise.

  Does *not* check to see if the two entities are *actually* colliding!!!!"
  [entity :- Entity
   another-entity :- Entity]
  (let [one-way-collision-check (sm/fn :- s/Bool
                                  [a :- Entity
                                   b :- Entity]
                                  (and
                                    (contains? a :collision)
                                    (not= (a :id) (b :id))
                                    (if (contains? (a :collision) :collides-with)
                                      (contains? (safe-get-in a [:collision :collides-with])
                                                 (safe-get-in b [:collision :type]))
                                      true)))]
    (and (one-way-collision-check entity another-entity)
         (one-way-collision-check another-entity entity))))

(sm/defn find-contacting-entity :- (s/maybe Entity)
  "Takes an Entity (one you're trying to move from one place to another) and a list of all of the
  Entities in the game. Returns another Entity if the space `entity` is trying to occupy is already filled,
  nil if the space `entity` is trying to occupy is empty."
  [entity :- Entity
   all-entities :- [Entity]]
  (let [collidable-entities (filter (partial entities-can-collide? entity)
                                    all-entities)]
    (first (filter #(shapes-collide? (% :shape) (entity :shape))
                   collidable-entities))))

(sm/defn find-closest-clear-spot :- (s/maybe s/Num)
  [event :- Event
   contacted-entity :- Entity]
  "Takes an :intended-movement event (for entity A) and the Entity that occupies the position that entity A
  is trying to move to (entity B). Finds the closest x- or y-value (depending on the value of `(event :axis)`)
  that entity A can occupy without contacting entity B and returns it if entity A fits there, or returns nil
  if no open spot exists."
  ; TODO - only supports rectangles
  (let [shape1 (safe-get-in event [:entity :shape])
        shape2 (contacted-entity :shape)
        arithmetic-fn (if (pos? (event :new-velocity)) - +)
        field (if (= (event :axis) :x) :width :height)
        axis-value-to-try (arithmetic-fn (shape2 (event :axis))
                                         (/ (shape2 field) 2)
                                         (/ (shape1 field) 2)
                                         0.01)]
    (when-not (find-contacting-entity (assoc-in (event :entity)
                                                [:shape (event :axis)]
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
                               (assoc-in [:shape axis] new-position)
                               (assoc-in [:motion :velocity axis] new-velocity)))]
    (update-entity! (entity :id) :collision-system update-entity-fn)
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
                               [:shape (event :axis)]
                               (event :new-position))]

    (if-let [contacted-entity (find-contacting-entity moved-entity (event :all-entities))]
      (handle-contact event contacted-entity)
      (apply-movement entity
                      (event :axis)
                      (event :new-position)
                      (event :new-velocity)))))

;; System definition

(sm/def collision-system :- System
  {:event-handlers [{:event-type :intended-movement
                     :fn         handle-intended-movement}]})
