(ns voke.system.collision
  (:require [schema.core :as s]
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
  (let [entity-shape (entity :shape)
        collidable-entities (filter (fn [another-entity]
                                      (and
                                        (contains? entity :collision)
                                        (not= (entity :id) (another-entity :id))))
                                    all-entities)]
    (first (filter #(shapes-collide? (% :shape)
                                     entity-shape)
                   collidable-entities))))

(sm/defn handle-intended-movement
  [event :- Event
   publish-chan]
  (let [entity (event :moved-entity)]
    (if-let [contacted-entity (find-contacting-entity entity (event :all-entities))]
      ; New position wasn't clear.
      (do
        ; Slow this guy down.
        (publish-event publish-chan {:event-type :update-entity
                                     :origin :collision-system
                                     :entity-id (entity :id)
                                     :fn (fn [old-entity]
                                           (-> old-entity
                                               (update-in [:motion :velocity :x] #(* % 0.5))
                                               (update-in [:motion :velocity :y] #(* % 0.5))))})

        ; Notify the rest of the world that a contact event occurred.
        (publish-event publish-chan {:event-type :contact
                                    :entities   [entity contacted-entity]}))

      ; Position was clear.
      (do
        ; Go ahead and apply the intended movement.
        (publish-event publish-chan {:event-type :update-entity
                                     :origin     :collision-system
                                     :entity-id  (entity :id)
                                     :fn         (fn [old-entity]
                                                   (-> old-entity
                                                       (update-in [:shape] merge (entity :shape))
                                                       (update-in [:motion] merge (entity :motion))))})
        (publish-event publish-chan {:event-type :movement
                                     :entity     entity})))))

;; System definition

(sm/def collision-system :- System
  {:event-handlers [{:event-type :intended-movement
                     :fn         handle-intended-movement}]})
