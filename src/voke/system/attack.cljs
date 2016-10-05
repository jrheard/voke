(ns voke.system.attack
  (:require [cljs.spec :as s]
            [voke.clock :refer [now]]
            [voke.entity :refer [projectile]]
            [voke.state :refer [add-entity!]]
            [voke.util :refer [bound-between]]))

(def maximum-entity-velocity-shot-speed-contribution 2)

(def things-all-projectiles-collide-with
  #{:obstacle
    :item})

(defn can-attack?
  [entity]
  (let [weapon (entity :component/weapon)]
    (and
      (get-in entity [:component/weapon :weapon/fire-direction])
      (>= (- (now)
             (weapon :weapon/last-attack-timestamp))
          (/ 1000
             (weapon :weapon/shots-per-second))))))

(s/fdef can-attack?
  :args (s/cat :entity :entity/entity)
  :ret boolean?)

(defn entity-velocity-contribution
  [entity axis]
  (if (contains? entity :component/motion)
    (bound-between (get-in entity [:component/motion :motion/velocity axis])
                   (- maximum-entity-velocity-shot-speed-contribution)
                   maximum-entity-velocity-shot-speed-contribution)
    0))

(s/fdef entity-velocity-contribution
  :args (s/cat :entity :entity/entity
               :axis :geometry/axis)
  :ret number?)

(defn shot-speed-on-axis
  [entity axis]
  (let [weapon (entity :component/weapon)
        trig-fn (if (= axis :geometry/x) Math/cos Math/sin)]
    (+ (* (trig-fn (weapon :weapon/fire-direction))
          (weapon :weapon/shot-speed))
       (entity-velocity-contribution entity axis))))

(s/fdef shot-speed-on-axis
  :args (s/cat :entity :entity/entity
               :axis :geometry/axis))

(defn -projectile-collides-with
  "Returns the set of things that an entity's projectiles collide with."
  [entity]
  (conj things-all-projectiles-collide-with
        (if (= (get-in entity [:component/collision :collision/type]) :good-guy)
          :bad-guy
          :good-guy)))

(s/fdef -projectile-collides-with
  :args (s/cat :entity :entity/entity)
  :ret :collision/collides-with)

; TODO what does this function do? document it
(defn -halves-along-dimension
  [entity axis]
  (let [side (if (= axis :geometry/x) :shape/width :shape/height)]
    (/ (+ (get-in entity [:component/shape side])
          (get-in entity [:component/weapon :weapon/projectile-shape side]))
       2)))

(s/fdef -halves-along-dimension
  :args (s/cat :entity :entity/entity
               :axis :geometry/axis)
  :ret number?)

(defn -pi-over
  [numerator denominator]
  (/ (* Math/PI numerator) denominator))

(defn -projectile-center
  [entity]
  (let [direction (get-in entity [:component/weapon :weapon/fire-direction])
        emission-direction (cond
                             (<= (-pi-over -3 4) direction (-pi-over -1 4)) :up
                             (<= (-pi-over -1 4) direction (-pi-over 1 4)) :right
                             (<= (-pi-over 1 4) direction (-pi-over 3 4)) :down
                             :else :left)
        center (get-in entity [:component/shape :shape/center])]
    {:geometry/x (condp = emission-direction
                   :left (- (center :geometry/x) (-halves-along-dimension entity :geometry/x))
                   :right (+ (center :geometry/x) (-halves-along-dimension entity :geometry/x))
                   (center :geometry/x))
     :geometry/y (condp = emission-direction
                   :up (- (center :geometry/y) (-halves-along-dimension entity :geometry/y))
                   :down (+ (center :geometry/y) (-halves-along-dimension entity :geometry/y))
                   (center :geometry/y))}))

(s/fdef -projectile-center
  :args (s/and (s/cat :entity :entity/entity)
               #(contains? (% :entity) :component/weapon)
               #(contains? (% :entity) :component/shape))
  :ret :geometry/vector2)

(defn make-projectile-for-entity
  [entity]
  (projectile (entity :entity/id)
              (-projectile-center entity)
              (-projectile-collides-with entity)
              (get-in entity [:component/weapon :weapon/projectile-shape])
              (get-in entity [:component/weapon :weapon/projectile-color])
              (shot-speed-on-axis entity :geometry/x)
              (shot-speed-on-axis entity :geometry/y)))

(s/fdef make-projectile-for-entity
  :args (s/and (s/cat :entity :entity/entity)
               #(contains? (% :entity) :component/weapon)
               #(contains? (% :entity) :component/shape)
               #(contains? (% :entity) :component/collision))
  :ret :entity/entity)

(defn process-firing-entities
  [entities]
  (flatten
    (for [entity (filter can-attack? entities)]
      (do
        ; (Hopefully) the closest thing to a `(fire-missiles!)` call I'll ever write
        (add-entity! (make-projectile-for-entity entity) :attack-system)
        (assoc-in entity [:component/weapon :weapon/last-attack-timestamp] (now))))))

(s/fdef process-firing-entities
  :args (s/cat :entities (s/coll-of :entity/entity))
  :ret (s/coll-of :entity/entity))

;; System definition

(def attack-system
  {:system/tick-fn process-firing-entities})
