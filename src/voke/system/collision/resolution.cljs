(ns voke.system.collision.resolution
  (:require [cljs.spec :as s]
            [clojure.set :refer [intersection]]
            [voke.system.collision.util :refer [apply-movement find-contacting-entities remove-entity!]]
            [voke.util :refer [winnow]]))

; Austin told me not to write your own collision system. I should have listened.

(defn left-edge-x [rect] (- (get-in rect [:shape/center :geometry/x])
                            (/ (rect :shape/width) 2)))
(defn right-edge-x [rect] (+ (get-in rect [:shape/center :geometry/x])
                             (/ (rect :shape/width) 2)))
(defn top-edge-y [rect] (- (get-in rect [:shape/center :geometry/y])
                           (/ (rect :shape/height) 2)))
(defn bottom-edge-y [rect] (+ (get-in rect [:shape/center :geometry/y])
                              (/ (rect :shape/height) 2)))

;;;;;
; Single-axis-handling logic

(defn find-closest-contacted-entity
  ; TODO test this!!! it rarely gets executed because contacted-entities is almost always a list of 1 item
  ; and so reduce just picks that one item without executing the reducing function
  [axis new-velocity contacted-entities]
  (let [[relevant-side-fn comparator] (case [axis (pos? new-velocity)]
                                        [:geometry/x false] [right-edge-x >]
                                        [:geometry/x true] [left-edge-x <]
                                        [:geometry/y false] [bottom-edge-y >]
                                        [:geometry/y true] [top-edge-y <])]
    (reduce (fn [current-winner contacted-entity]
              (if (comparator (relevant-side-fn contacted-entity)
                              (relevant-side-fn current-winner))
                contacted-entity
                current-winner))
            contacted-entities)))

(s/fdef find-closest-contacted-entity
  :args (s/cat :axis #{:geometry/x :geometry/y}
               :new-velocity number?
               :contacted-entities (s/coll-of :entity/entity))
  :ret :entity/entity)

(defn find-closest-clear-spot
  [entity axis new-velocity contacted-entities]
  (let [closest-entity (find-closest-contacted-entity axis new-velocity contacted-entities)
        shape1 (entity :component/shape)
        shape2 (closest-entity :component/shape)
        arithmetic-fn (if (pos? new-velocity) - +)
        field (if (= axis :geometry/x) :shape/width :shape/height)]
    (arithmetic-fn (get-in shape2 [:shape/center axis])
                   (/ (shape2 field) 2)
                   (/ (shape1 field) 2)
                   ; XXX - diagonal resolution uses a fudge factor of 0.1,
                   ; single-axis resolution uses 0.01. standardize
                   0.01)))

(s/fdef find-closest-clear-spot
  :args (s/cat :entity :entity/entity
               :axis #{:geometry/x :geometry/y}
               :new-velocity number?
               :contacted-entities (s/coll-of :entity/entity)))

(defn find-new-position-and-velocity-on-axis
  [entity new-center new-velocity axis remaining-contacted-entities all-entities]
  (let [entities-contacted-on-this-axis (find-contacting-entities entity
                                                                  (assoc (get-in entity [:component/shape :shape/center])
                                                                         axis
                                                                         new-center)
                                                                  all-entities)
        contacted-entities (intersection (set entities-contacted-on-this-axis)
                                         (set remaining-contacted-entities))]
    (if (seq contacted-entities)
      [(find-closest-clear-spot entity axis new-velocity contacted-entities) 0 false]
      [new-center new-velocity true])))

(s/fdef find-new-position-and-velocity-on-axis
  :args (s/cat :entity :entity/entity
               :new-center number?
               :new-velocity number?
               :axis #{:geometry/x :geometry/y}
               :remaining-contacted-entities (s/coll-of :entity/entity)
               :all-entities (s/coll-of :entity/entity))
  :ret (s/cat :new-position number?
              :new-velocity number?
              :clear-on-this-axis boolean?))


;;;;;
; Corner-collision-handling logic

; We only support axis-aligned bounding boxes, so entities' component lines always look like
; y = 4 or x = -2.021, etc.
(s/def ::axis #{:geometry/x :geometry/y})
(s/def ::axis-line-value number?)
(s/def ::axis-aligned-line (s/keys :req [::axis ::axis-line-value]))

(defn entity-to-lines
  [entity]
  (let [shape (entity :component/shape)]
    [{::axis :geometry/x ::axis-line-value (left-edge-x shape)}
     {::axis :geometry/x ::axis-line-value (right-edge-x shape)}
     {::axis :geometry/y ::axis-line-value (top-edge-y shape)}
     {::axis :geometry/y ::axis-line-value (bottom-edge-y shape)}]))

(s/fdef entity-to-lines
  :args (s/cat :entity :entity/entity)
  :ret (s/coll-of ::axis-aligned-line))

(defn find-intersection
  [slope intercept line]
  (case (line ::axis)
    :geometry/x {:geometry/x (line ::axis-line-value)
                 :geometry/y (+ (* slope
                                   (line ::axis-line-value))
                                intercept)}
    :geometry/y {:geometry/x (/ (- (line ::axis-line-value)
                                   intercept)
                                slope)
                 :geometry/y (line ::axis-line-value)}))

(s/fdef find-intersection
  :args (s/cat :slope number?
               :intercept number?
               :line ::axis-aligned-line)
  :ret :geometry/vector2)

(defn distance-between-points
  [a b]
  (Math/sqrt (+ (Math/pow (- (a :geometry/x) (b :geometry/x))
                          2)
                (Math/pow (- (a :geometry/y) (b :geometry/y))
                          2))))

(s/fdef distance-between-points
  :args (s/cat :a :geometry/vector2
               :b :geometry/vector2)
  :ret number?)

(defn get-leading-corner
  [entity new-velocity]
  (let [x-operation (if (pos? (new-velocity :geometry/x)) + -)
        y-operation (if (pos? (new-velocity :geometry/y)) + -)]
    {:geometry/x (x-operation (get-in entity [:component/shape :shape/center :geometry/x])
                              (/ (get-in entity [:component/shape :shape/width]) 2))
     :geometry/y (y-operation (get-in entity [:component/shape :shape/center :geometry/y])
                              (/ (get-in entity [:component/shape :shape/height]) 2))}))

(s/fdef get-leading-corner
  :args (s/cat :entity :entity/entity
               :new-velocity number?)
  :ret :geometry/vector2)

(defn resolve-diagonal-collision
  [entity new-velocity remaining-contacted-entities]
  ; If you're invoking this function, you've found yourself in a situation where you've got
  ; an entity that's trying to move diagonally but has collided with one or more other entities.

  ; We start by breaking down those other entities into their component AxisAlignedLines.
  (let [lines (mapcat entity-to-lines remaining-contacted-entities)

        ; We then find the leading corner of the entity you're trying to move
        ; - so if it's trying to move up and to the right, that's its top-right corner -
        ; and then we find the line described by that corner's position and the direction it's moving in.
        leading-corner (get-leading-corner entity new-velocity)
        velocity-line-slope (/ (new-velocity :geometry/y) (new-velocity :geometry/x))
        ; y = mx + b, so b = y - mx
        velocity-line-intercept (- (leading-corner :geometry/y)
                                   (* velocity-line-slope
                                      (leading-corner :geometry/x)))

        ; Now, all we have to do is take that line, find all of the places where it intersects with
        ; the other entities' AxisAlignedLines, and find the closest of those intersections.
        ; That's the closest spot that it's safe for us to move this entity's leading corner to.
        intersections (map (partial find-intersection velocity-line-slope velocity-line-intercept)
                           lines)
        closest-intersection (apply min-key
                                    (partial distance-between-points leading-corner)
                                    intersections)
        x-operation (if (pos? (new-velocity :geometry/x)) - +)
        y-operation (if (pos? (new-velocity :geometry/y)) - +)]

    (apply-movement entity
                    {:geometry/x (x-operation (closest-intersection :geometry/x)
                                              (/ (get-in entity [:component/shape :shape/width]) 2)
                                              0.1)
                     :geometry/y (y-operation (closest-intersection :geometry/y)
                                              (/ (get-in entity [:component/shape :shape/height]) 2)
                                              0.1)}
                    {:geometry/x 0 :geometry/y 0})))

(s/fdef resolve-diagonal-collision
  :args (s/cat :entity :entity/entity
               :new-velocity :geometry/vector2
               :remaining-contacted-entities (s/coll-of :entity-entity)))

(defn move-to-nearest-clear-spot
  [entity new-center new-velocity remaining-contacted-entities all-entities]
  ; We start by checking on the :x and :y axes, one at a time, to see if it's possible
  ; for this entity to make its intended movement on that single axis without colliding
  ; with anything. This is what lets us support behavior where the player's right side
  ; is touching a wall, and they're holding up+right, and they expect to be able to
  ; go up even though they're blocked on their right side.
  (let [finder (fn [axis]
                 (find-new-position-and-velocity-on-axis entity
                                                         (new-center axis)
                                                         (new-velocity axis)
                                                         axis
                                                         remaining-contacted-entities
                                                         all-entities))
        [x-position x-velocity x-axis-clear] (finder :geometry/x)
        [y-position y-velocity y-axis-clear] (finder :geometry/y)]
    (if (and x-axis-clear y-axis-clear)
      ; If we've gotten here, then each individual axis _appears_ to be clear; but since we're
      ; handling a collision in the first place, we know that `new-center` is absolutely _not_ a valid place
      ; to move to. We've got a corner collision, so let's go ahead and handle that specially.
      (resolve-diagonal-collision entity
                                  new-velocity
                                  remaining-contacted-entities)

      (apply-movement entity
                      {:geometry/x x-position
                       :geometry/y y-position}
                      {:geometry/x x-velocity
                       :geometry/y y-velocity}))))

(s/fdef move-to-nearest-clear-spot
  :args (s/cat :entity :entity/entity
               :new-center :geometry/vector2
               :new-velocity :geometry/vector2
               :remaining-contacted-entities (s/coll-of :entity/entity)
               :all-entities (s/coll-of :entity/entity)))

(defn resolve-collision
  [entity new-center new-velocity contacted-entities all-entities]
  "Resolves a collision between `entity` and `contacted-entities`. Destroys any :destroyed-on-contact entities
  involved in the collision, and makes some decisions about the final location of all other entities involved."
  (let [[entities-to-destroy remaining-entities] (winnow #(get-in % [:component/collision :collision/destroyed-on-contact])
                                                         contacted-entities)]
    ; We're processing a collision, so go ahead and nuke everything that's supposed to be
    ; destroyed during collisions.
    (doseq [entity-to-destroy entities-to-destroy]
      (remove-entity! entity-to-destroy))

    (if (get-in entity [:component/collision :collision/destroyed-on-contact])
      ; entity's supposed to be destroyed, too; destroy it and we're done.
      (remove-entity! entity)

      ; entity isn't supposed to be destroyed, so let's figure out where to put it.
      (if (seq remaining-entities)
        ; There's at least one remaining non-destroyed entity from the set of entities
        ; that we collided with earlier.
        (move-to-nearest-clear-spot entity new-center new-velocity remaining-entities all-entities)

        ; There's nothing left! Go ahead and make the move that this entity was trying to make in the first place.
        (apply-movement entity new-center new-velocity)))))

(s/fdef resolve-collision
  :args (s/cat :entity :entity/entity
               :new-center :geometry/vector2
               :new-velocity :geometry/vector2
               :contacted-entities (s/coll-of :entity/entity)
               :all-entities (s/coll-of :entity/entity)))
