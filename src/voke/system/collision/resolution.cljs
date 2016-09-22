(ns voke.system.collision.resolution
  (:require [clojure.set :refer [intersection]]
            [schema.core :as s]
            [voke.schemas :refer [Axis Entity Vector2]]
            [voke.system.collision.util :refer [apply-movement find-contacting-entities remove-entity!]]
            [voke.util :refer [winnow]])
  (:require-macros [schema.core :as sm]))

; Austin told me not to write your own collision system. I should have listened.

(defn left-edge-x [rect] (- (get-in rect [:center :x])
                            (/ (rect :width) 2)))
(defn right-edge-x [rect] (+ (get-in rect [:center :x])
                             (/ (rect :width) 2)))
(defn top-edge-y [rect] (- (get-in rect [:center :y])
                           (/ (rect :height) 2)))
(defn bottom-edge-y [rect] (+ (get-in rect [:center :y])
                              (/ (rect :height) 2)))

(sm/defn find-closest-contacted-entity
  ; TODO test this!!! it rarely gets executed because contacted-entities is almost always a list of 1 item
  ; and so reduce just picks that one item without executing the reducing function
  [axis :- Axis
   new-velocity :- s/Num
   contacted-entities :- [Entity]]
  (let [[relevant-side-fn comparator] (case [axis (pos? new-velocity)]
                                        [:x false] [right-edge-x >]
                                        [:x true] [left-edge-x <]
                                        [:y false] [bottom-edge-y >]
                                        [:y true] [top-edge-y <])]
    (reduce (fn [current-winner contacted-entity]
              (if (comparator (relevant-side-fn contacted-entity)
                              (relevant-side-fn current-winner))
                contacted-entity
                current-winner))
            contacted-entities)))

(sm/defn find-closest-clear-spot :- s/Num
  [entity :- Entity
   axis :- Axis
   new-velocity :- s/Num
   contacted-entities :- [Entity]]
  (let [closest-entity (find-closest-contacted-entity axis new-velocity contacted-entities)
        shape1 (entity :shape)
        shape2 (closest-entity :shape)
        arithmetic-fn (if (pos? new-velocity) - +)
        field (if (= axis :x) :width :height)]
    (arithmetic-fn (get-in shape2 [:center axis])
                   (/ (shape2 field) 2)
                   (/ (shape1 field) 2)
                   0.01)))

(sm/defn find-new-position-and-velocity-on-axis :- [(s/one s/Num "new-position")
                                                    (s/one s/Num "new-velocity")]
  [entity :- Entity
   new-center :- s/Num
   new-velocity :- s/Num
   axis :- Axis
   remaining-contacted-entities :- [Entity]
   all-entities :- [Entity]]
  (let [entities-contacted-on-this-axis (find-contacting-entities entity
                                                                  (assoc (get-in entity [:shape :center])
                                                                         axis
                                                                         new-center)
                                                                  all-entities)
        contacted-entities (intersection (set entities-contacted-on-this-axis)
                                         (set remaining-contacted-entities))]
    (if (seq contacted-entities)
      [(find-closest-clear-spot entity axis new-velocity contacted-entities) 0]
      [new-center new-velocity])))

; We only support axis-aligned bounding boxes, so entities' component lines always look like
; y = 4 or x = -2.021, etc.
(sm/defschema AxisAlignedLine {:axis  Axis
                               :value s/Num})

(sm/defn entity-to-lines :- [AxisAlignedLine]
  [entity :- Entity]
  (let [shape (entity :shape)]
    [{:axis :x :value (left-edge-x shape)}
     {:axis :x :value (right-edge-x shape)}
     {:axis :y :value (top-edge-y shape)}
     {:axis :y :value (bottom-edge-y shape)}]))

(sm/defn find-intersection :- Vector2
  [slope :- s/Num
   intercept :- s/Num
   line :- AxisAlignedLine]
  (case (line :axis)
    :x {:x (line :value)
        :y (+ (* slope
                 (line :value))
              intercept)}
    :y {:x (/ (- (line :value)
                 intercept)
              slope)
        :y (line :value)}))

(sm/defn distance-between-points :- s/Num
  [a :- Vector2
   b :- Vector2]
  (Math/sqrt (+ (Math/pow (- (a :x) (b :x))
                          2)
                (Math/pow (- (a :y) (b :y))
                          2))))

(sm/defn get-leading-corner :- Vector2
  [entity :- Entity
   new-velocity :- Vector2]
  (let [x-operation (if (pos? (new-velocity :x)) + -)
        y-operation (if (pos? (new-velocity :y)) + -)]
    {:x (x-operation (get-in entity [:shape :center :x])
                     (/ (get-in entity [:shape :width]) 2))
     :y (y-operation (get-in entity [:shape :center :y])
                     (/ (get-in entity [:shape :height]) 2))}))

(sm/defn resolve-diagonal-collision
  "TODO document me - translate 3/31 notes into documentation"
  [entity :- Entity
   new-velocity :- Vector2
   remaining-contacted-entities :- [Entity]]
  (let [lines (mapcat entity-to-lines remaining-contacted-entities)
        velocity-line-slope (/ (new-velocity :y) (new-velocity :x))
        leading-corner (get-leading-corner entity new-velocity)
        ; y = mx + b, so b = y - mx
        velocity-line-intercept (- (leading-corner :y)
                                   (* velocity-line-slope
                                      (leading-corner :x)))
        intersections (map (partial find-intersection velocity-line-slope velocity-line-intercept)
                           lines)
        closest-intersection (apply min-key
                                    (partial distance-between-points leading-corner)
                                    intersections)
        x-operation (if (pos? (new-velocity :x)) - +)
        y-operation (if (pos? (new-velocity :y)) - +)]

    (apply-movement entity
                    {:x (x-operation (closest-intersection :x)
                                     (/ (get-in entity [:shape :width]) 2)
                                     0.1)
                     :y (y-operation (closest-intersection :y)
                                     (/ (get-in entity [:shape :height]) 2)
                                     0.1)}
                    {:x 0 :y 0})))

(sm/defn move-to-nearest-clear-spot
  [entity :- Entity
   new-center :- Vector2
   new-velocity :- Vector2
   remaining-contacted-entities :- [Entity]
   all-entities :- [Entity]]
  (let [finder (fn [axis]
                 (find-new-position-and-velocity-on-axis entity
                                                         (new-center axis)
                                                         (new-velocity axis)
                                                         axis
                                                         remaining-contacted-entities
                                                         all-entities))
        [x-position x-velocity] (finder :x)
        [y-position y-velocity] (finder :y)
        nearest-clear-spot {:x x-position
                            :y y-position}]
    (if (= nearest-clear-spot new-center)
      (resolve-diagonal-collision entity
                                  new-velocity
                                  remaining-contacted-entities)

      (apply-movement entity
                      nearest-clear-spot
                      {:x x-velocity
                       :y y-velocity}))))

(sm/defn resolve-collision
  [entity :- Entity
   new-center :- Vector2
   new-velocity :- Vector2
   contacted-entities :- [Entity]
   all-entities :- [Entity]]
  "Resolves a collision between `entity` and `contacted-entities`. Destroys any :destroyed-on-contact entities
  involved in the collision, and makes some decisions about the final location of all other entities involved."
  (let [[entities-to-destroy remaining-entities] (winnow #(get-in % [:collision :destroyed-on-contact])
                                                         contacted-entities)]
    ; We're processing a collision, so go ahead and nuke everything that's supposed to be
    ; destroyed during collisions.
    (doseq [entity-to-destroy entities-to-destroy]
      (remove-entity! entity-to-destroy))

    (if (get-in entity [:collision :destroyed-on-contact])
      ; entity's supposed to be destroyed, too; destroy it and we're done.
      (remove-entity! entity)

      ; entity isn't supposed to be destroyed, so let's figure out where to put it.
      (if (seq remaining-entities)
        ; There's at least one remaining non-destroyed entity from the set of entities
        ; that we collided with earlier.
        (move-to-nearest-clear-spot entity new-center new-velocity remaining-entities all-entities)

        ; There's nothing left! Go ahead and make the move that this entity was trying to make in the first place.
        (apply-movement entity new-center new-velocity)))))
