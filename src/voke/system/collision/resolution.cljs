(ns voke.system.collision.resolution
  (:require [schema.core :as s]
            [voke.schemas :refer [Axis Entity Vector2]]
            [voke.system.collision.utils :refer [apply-movement find-contacting-entities]])
  (:require-macros [schema.core :as sm]))

(defn left-edge-x [rect] (- (rect :x)
                            (/ (rect :width) 2)))
(defn right-edge-x [rect] (+ (rect :x)
                             (/ (rect :width) 2)))
(defn top-edge-y [rect] (- (rect :y)
                           (/ (rect :height) 2)))
(defn bottom-edge-y [rect] (+ (rect :y)
                              (/ (rect :height) 2)))

(sm/defn find-closest-contacted-entity
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

(sm/defn move-to-closest-clear-spot
  [entity :- Entity
   axis :- Axis
   new-velocity :- s/Num
   contacted-entities :- [Entity]]
  (let [closest-entity (find-closest-contacted-entity axis new-velocity contacted-entities)
        shape1 (entity :shape)
        shape2 (closest-entity :shape)
        arithmetic-fn (if (pos? new-velocity) - +)
        field (if (= axis :x) :width :height)
        axis-value (arithmetic-fn (get-in shape2 [:center axis])
                                  (/ (shape2 field) 2)
                                  (/ (shape1 field) 2)
                                  0.01)]
    (apply-movement entity {axis axis-value} {axis 0})))

(sm/defn move-in-direction
  [entity :- Entity
   new-center :- s/Num
   new-velocity :- s/Num
   axis :- Axis
   all-entities :- [Entity]]
  (let [contacted-entities (find-contacting-entities entity
                                                        (assoc (get-in entity [:shape :center])
                                                               axis
                                                               new-center)
                                                        all-entities)]
    (if (seq contacted-entities)
      (move-to-closest-clear-spot entity axis new-velocity contacted-entities)
      (apply-movement entity {axis new-center} {axis new-velocity}))))

(sm/defn resolve-collision
  [entity :- Entity
   new-center :- Vector2
   new-velocity :- Vector2
   all-entities :- [Entity]]
  (move-in-direction entity (new-center :x) (new-velocity :x) :x all-entities)
  (move-in-direction entity (new-center :y) (new-velocity :y) :y all-entities))
