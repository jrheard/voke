(ns voke.system.collision
  (:require [cljs.core.match]
            [schema.core :as s]
            [voke.events :refer [publish-event]]
            [voke.schemas :refer [Entity Event System]])
  (:require-macros [cljs.core.match :refer [match]]
                   [schema.core :as sm]))

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


; TODO dear god all the code i added in this commit is fuckin nuts and insane
; TODO REFACTOR ALL THIS

(sm/defn find-closest-clear-spot :- (s/maybe s/Num)
  [event :- Event
   contacted-entity :- Entity]

  (let [; TODO AABBs for non-rect shapes
        shape1 (get-in event [:entity :shape])
        shape2 (contacted-entity :shape)
        axis-value-to-try (match [(event :axis) (pos? (event :new-velocity))]
                            ; TODO refactor
                            [:x true] (- (shape2 :x)
                                         (/ (shape2 :width) 2)
                                         (/ (shape1 :width) 2)
                                         0.01)
                            [:x false] (+ (shape2 :x)
                                          (/ (shape2 :width) 2)
                                          (/ (shape1 :width) 2)
                                          0.01)
                            [:y true] (- (shape2 :y)
                                         (/ (shape2 :height) 2)
                                         (/ (shape1 :height) 2)
                                         0.01)
                            [:y false] (+ (shape2 :y)
                                          (/ (shape2 :height) 2)
                                          (/ (shape1 :height) 2)
                                          0.01))]
    (when-not (find-contacting-entity (assoc-in (event :entity)
                                                [:shape (event :axis)]
                                                axis-value-to-try)
                                      (event :all-entities))
      axis-value-to-try)))

(sm/defn handle-contact
  [event :- Event
   contacted-entity :- Entity
   publish-chan]
  (let [fire-update-event (fn [entity-update-fn]
                            (publish-event publish-chan {:event-type :update-entity
                                                         :origin     :collision-system
                                                         :entity-id  ((event :entity) :id)
                                                         :fn         entity-update-fn}))
        closest-clear-spot (find-closest-clear-spot event contacted-entity)]

    (if closest-clear-spot
      ; Great, we found a clear spot! Move there and stand still.
      (do
        (fire-update-event (fn [old-entity]
                             (-> old-entity
                                 (assoc-in [:shape (event :axis)] closest-clear-spot)
                                 (assoc-in [:motion :velocity (event :axis)] 0))))
        (publish-event publish-chan {:event-type :movement
                                     :entity     (assoc-in (event :entity)
                                                           [:shape (event :axis)]
                                                           closest-clear-spot)}))

      ; Couldn't find a clear spot; slow down and let him try moving again next tick.
      (fire-update-event (fn [old-entity]
                           (update-in old-entity
                                      [:motion :velocity (event :axis)]
                                      #(* % 0.7)))))))

(sm/defn handle-intended-movement
  [event :- Event
   publish-chan]
  (let [entity (event :entity)
        axis (event :axis)
        moved-entity (assoc-in entity
                               [:shape axis]
                               (event :new-position))]
    (if-let [contacted-entity (find-contacting-entity moved-entity (event :all-entities))]

      ; New position wasn't clear.
      (do
        (handle-contact event contacted-entity publish-chan)

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
                                                       (assoc-in [:shape axis]
                                                                 (event :new-position))
                                                       (assoc-in [:motion :velocity axis]
                                                                 (event :new-velocity))))})
        (publish-event publish-chan {:event-type :movement
                                     :entity     entity})))))

;; System definition

(sm/def collision-system :- System
  {:event-handlers [{:event-type :intended-movement
                     :fn         handle-intended-movement}]})
