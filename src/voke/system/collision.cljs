(ns voke.system.collision
  (:require [plumbing.core :refer [safe-get-in]]
            [schema.core :as s]
            [voke.events :refer [publish-event]]
            [voke.schemas :refer [Entity Event System]])
  (:require-macros [schema.core :as sm]))

(defn left-edge-x [rect] (- (rect :x)
                            (/ (rect :width) 2)))
(defn right-edge-x [rect] (+ (rect :x)
                             (/ (rect :width) 2)))
(defn top-edge-y [rect] (- (rect :y)
                           (/ (rect :height) 2)))
(defn bottom-edge-y [rect] (+ (rect :y)
                              (/ (rect :height) 2)))

(sm/defn shapes-collide? :- s/Bool
  [shape1 shape2]
  ; right now everything's just aabbs
  ; when that changes, this function will need to get smarter
  (not-any? identity
            [(< (bottom-edge-y shape1) (top-edge-y shape2))
             (> (top-edge-y shape1) (bottom-edge-y shape2))
             (> (left-edge-x shape1) (right-edge-x shape2))
             (< (right-edge-x shape1) (left-edge-x shape2))]))

(sm/defn find-contacting-entity :- (s/maybe Entity)
  "Takes an Entity (one you're trying to move from one place to another) and a list of all of the
  Entities in the game. Returns another Entity if the space `entity` is trying to occupy is already filled,
  nil if the space `entity` is trying to occupy is empty."
  [entity :- Entity
   all-entities :- [Entity]]
  (let [collidable-entities (filter (fn [another-entity]
                                      (and
                                        (contains? entity :collision)
                                        (not= (entity :id) (another-entity :id))))
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
   axis :- (s/enum :x :y)
   new-position :- s/Num
   new-velocity :- s/Num
   publish-chan]
  "Fires events to notify the world that a particular entity should have a new position+velocity."
  (let [update-entity-fn (fn [entity]
                           (-> entity
                               (assoc-in [:shape axis] new-position)
                               (assoc-in [:motion :velocity axis] new-velocity)))]
    (publish-event publish-chan {:event-type :update-entity
                                 :origin     :collision-system
                                 :entity-id  (entity :id)
                                 :fn         update-entity-fn})
    (publish-event publish-chan {:event-type :movement
                                 :entity     (update-entity-fn entity)})))

(sm/defn handle-contact
  [event :- Event
   contacted-entity :- Entity
   publish-chan]
  (if-let [closest-clear-spot (find-closest-clear-spot event contacted-entity)]
    ; Great, we found a clear spot! Move there and stand still.
    (apply-movement (event :entity)
                    (event :axis)
                    closest-clear-spot
                    0
                    publish-chan)

    ; Couldn't find a clear spot; slow him down, he can try moving again next tick.
    (publish-event publish-chan {:event-type :update-entity
                                 :origin     :collision-system
                                 :entity-id  ((event :entity) :id)
                                 :fn         (fn [old-entity]
                                               (update-in old-entity
                                                          [:motion :velocity (event :axis)]
                                                          #(* % 0.7)))}))

  (publish-event publish-chan {:event-type :contact
                               :entities   [(event :entity) contacted-entity]}))

(sm/defn handle-intended-movement
  [event :- Event
   publish-chan]
  (let [entity (event :entity)
        moved-entity (assoc-in entity
                               [:shape (event :axis)]
                               (event :new-position))]

    (if-let [contacted-entity (find-contacting-entity moved-entity (event :all-entities))]
      (handle-contact event contacted-entity publish-chan)
      (apply-movement entity
                      (event :axis)
                      (event :new-position)
                      (event :new-velocity)
                      publish-chan))))

;; System definition

(sm/def collision-system :- System
  {:event-handlers [{:event-type :intended-movement
                     :fn         handle-intended-movement}]})
