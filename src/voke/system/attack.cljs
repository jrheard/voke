(ns voke.system.attack
  (:require [plumbing.core :refer [safe-get-in]]
            [schema.core :as s]
            [voke.clock :refer [now]]
            [voke.entity :refer [projectile]]
            [voke.schemas :refer [Axis CollisionType Direction Entity System Vector2]]
            [voke.state :refer [add-entity!]]
            [voke.util :refer [bound-between]])
  (:require-macros [schema.core :as sm]))

(def maximum-entity-velocity-shot-speed-contribution 2)

;; everything in here is gross and that is ok for now

(sm/defn can-attack? :- s/Bool
  [entity :- Entity]
  (let [weapon (entity :weapon)]
    (and
      (get-in entity [:weapon :fire-direction])
      (>= (- (now)
             (weapon :last-attack-timestamp))
          (/ 1000
             (weapon :shots-per-second))))))

(sm/defn entity-velocity-contribution
  [entity :- Entity
   axis :- Axis]
  (bound-between (safe-get-in entity [:motion :velocity axis])
                 (- maximum-entity-velocity-shot-speed-contribution)
                 maximum-entity-velocity-shot-speed-contribution))

(sm/defn shot-speed
  [entity :- Entity
   axis :- Axis]
  (let [weapon (entity :weapon)
        trig-fn (if (= axis :x) Math/cos Math/sin)]
    (+ (* (trig-fn (weapon :fire-direction))
          (weapon :shot-speed))
       (entity-velocity-contribution entity axis))))

(sm/defn -collides-with :- #{CollisionType}
  [entity :- Entity]
  (hash-set
    :obstacle
    :item
    (if (= (get-in entity [:collision :type]) :good-guy)
      :bad-guy
      :good-guy)))

(sm/defn -halves-along-dimension :- [(s/one s/Num "entity half") (s/one s/Num "projectile half")]
  [entity :- Entity
   axis :- Axis]
  (let [side (if (= axis :x) :width :height)]
    [(/ (get-in entity [:shape side]) 2)
     (/ (get-in entity [:weapon :projectile-shape side]) 2)]))

(defn -pi-over
  [numerator denominator]
  (/ (* Math/PI numerator) denominator))

(sm/defn -projectile-center :- Vector2
  [entity :- Entity]
  (let [direction (get-in entity [:weapon :fire-direction])
        side (cond
               (<= (-pi-over -3 4) direction (-pi-over -1 4)) :up
               (<= (-pi-over -1 4) direction (-pi-over 1 4)) :right
               (<= (-pi-over 1 4) direction (-pi-over 3 4)) :down
               :else :left)
        center (get-in entity [:shape :center])]
    {:x (condp = side
          :left (apply - (center :x) (-halves-along-dimension entity :x))
          :right (apply + (center :x) (-halves-along-dimension entity :x))
          (center :x))
     :y (condp = side
          :up (apply - (center :y) (-halves-along-dimension entity :y))
          :down (apply + (center :y) (-halves-along-dimension entity :y))
          (center :y))}))

(sm/defn make-projectile-for-entity
  [entity :- Entity]
  (projectile (entity :id)
              (-projectile-center entity)
              (-collides-with entity)
              (safe-get-in entity [:weapon :projectile-shape])
              (safe-get-in entity [:weapon :projectile-color])
              (shot-speed entity :x)
              (shot-speed entity :y)))

(sm/defn process-firing-entities :- [Entity]
  [entities :- [Entity]]
  (flatten
    (for [entity (filter can-attack? entities)]
      (do
        (add-entity! (make-projectile-for-entity entity) :attack-system)
        (assoc-in entity [:weapon :last-attack-timestamp] (now))))))

;; System definition

(sm/def attack-system :- System
  {:tick-fn process-firing-entities})
