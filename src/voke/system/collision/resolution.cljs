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
   all-entities :- [Entity]]
  (let [contacted-entities (find-contacting-entities entity
                                                        (assoc (get-in entity [:shape :center])
                                                               axis
                                                               new-center)
                                                        all-entities)]
    (if (seq contacted-entities)
      [(find-closest-clear-spot entity axis new-velocity contacted-entities) 0]
      [new-center new-velocity])))

(sm/defn resolve-collision
  [entity :- Entity
   new-center :- Vector2
   new-velocity :- Vector2
   all-entities :- [Entity]]
  (let [finder (fn [axis]
                 (find-new-position-and-velocity-on-axis entity
                                                         (new-center axis)
                                                         (new-velocity axis)
                                                         axis
                                                         all-entities))
        [x-position x-velocity] (finder :x)
        [y-position y-velocity] (finder :y)]
    (apply-movement entity
                    {:x x-position
                     :y y-position}
                    {:x x-velocity
                     :y y-velocity})))
