(ns voke.system.collision.resolution
  (:require [clojure.set :refer [intersection]]
            [schema.core :as s]
            [voke.schemas :refer [Axis Entity Vector2]]
            [voke.system.collision.util :refer [apply-movement find-contacting-entities remove-entity!]]
            [voke.util :refer [winnow]])
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
    (when (= (entity :id) 0)
      (js/console.log "yo" (clj->js (shape1 :center)) (clj->js (shape2 :center)))
      (js/console.log (clj->js axis))
      (js/console.log
          (arithmetic-fn (get-in shape2 [:center axis])
                         (/ (shape2 field) 2)
                         (/ (shape1 field) 2)
                         0.01)
          )
      )
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
  (let [all-contacted-entities (find-contacting-entities entity
                                                         (assoc (get-in entity [:shape :center])
                                                                axis
                                                                new-center)
                                                         all-entities)
        contacted-entities (intersection (set all-contacted-entities)
                                         (set remaining-contacted-entities))]
    (if (seq contacted-entities)
      [(find-closest-clear-spot entity axis new-velocity contacted-entities) 0]
      [new-center new-velocity])))

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
        [y-position y-velocity] (finder :y)]
    (apply-movement entity
                    {:x x-position
                     :y y-position}
                    {:x x-velocity
                     :y y-velocity})))

(sm/defn resolve-collision
  [entity :- Entity
   new-center :- Vector2
   new-velocity :- Vector2
   contacted-entities :- [Entity]
   all-entities :- [Entity]]
  "Resolves a collision between `entity` and `contacted-entities`. Destroys any :destroyed-on-contact entities
  involved in the collision, and makes some decisions about the final location of all other entities involved."
  (js/console.log "resolving for" (entity :id))
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
