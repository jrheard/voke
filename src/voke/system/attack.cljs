(ns voke.system.attack
  (:require [cljs.spec :as s]
            [voke.clock :refer [now]]
            [voke.entity :refer [projectile]]
            [voke.state :refer [add-entity!]]
            [voke.util :refer [bound-between]]))

(def maximum-entity-velocity-shot-speed-contribution 2)

;; everything in here is gross and that is ok for now

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
               :axis #{:geometry/x :geometry/y})
  :ret number?)

(defn shot-speed
  [entity axis]
  (let [weapon (entity :component/weapon)
        trig-fn (if (= axis :geometry/x) Math/cos Math/sin)]
    (+ (* (trig-fn (weapon :weapon/fire-direction))
          (weapon :weapon/shot-speed))
       (entity-velocity-contribution entity axis))))

(s/fdef shot-speed
  :args (s/cat :entity :entity/entity
               :axis #{:geometry/x :geometry/y}))

; TODO why does this function exist? what is its purpose? docstring it
(defn -collides-with
  [entity]
  (hash-set
    :obstacle
    :item
    (if (= (get-in entity [:component/collision :collision/type]) :good-guy)
      :bad-guy
      :good-guy)))

(s/fdef -collides-with
  :args (s/cat :entity :entity/entity)
  :ret (s/coll-of :collision/type :kind set?))

; TODO what does this function do? document it
(defn -halves-along-dimension
  [entity axis]
  (let [side (if (= axis :geometry/x) :shape/width :shape/height)]
    [(/ (get-in entity [:component/shape side]) 2)
     (/ (get-in entity [:component/weapon :weapon/projectile-shape side]) 2)]))

(s/fdef -halves-along-dimension
  :args (s/cat :entity :entity/entity
               :axis #{:geometry/x :geometry/y})
  ; what the fuck are :entity-half and :projectile-half
  :ret (s/cat :entity-half number?
              :projectile-half number?))

(defn -pi-over
  [numerator denominator]
  (/ (* Math/PI numerator) denominator))

(defn -projectile-center
  [entity]
  (let [direction (get-in entity [:component/weapon :weapon/fire-direction])
        side (cond
               (<= (-pi-over -3 4) direction (-pi-over -1 4)) :up
               (<= (-pi-over -1 4) direction (-pi-over 1 4)) :right
               (<= (-pi-over 1 4) direction (-pi-over 3 4)) :down
               :else :left)
        center (get-in entity [:shape :center])]
    {:geometry/x (condp = side
                   :left (apply - (center :geometry/x) (-halves-along-dimension entity :geometry/x))
                   :right (apply + (center :geometry/x) (-halves-along-dimension entity :geometry/x))
                   (center :geometry/x))
     :geometry/y (condp = side
                   :up (apply - (center :geometry/y) (-halves-along-dimension entity :geometry/y))
                   :down (apply + (center :geometry/y) (-halves-along-dimension entity :geometry/y))
                   (center :geometry/y))}))

(s/fdef -projectile-center
  :args (s/and (s/cat :entity :entity/entity)
               #(contains? (% :entity) :component/weapon)
               #(contains? (% :entity) :component/shape))
  :ret :geometry/vector2)

(defn make-projectile-for-entity
  [entity]
  (projectile (entity :id)
              (-projectile-center entity)
              (-collides-with entity)
              (get-in entity [:component/weapon :weapon/projectile-shape])
              (get-in entity [:component/weapon :weapon/projectile-color])
              (shot-speed entity :geometry/x)
              (shot-speed entity :geometry/y)))

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
        (add-entity! (make-projectile-for-entity entity) :attack-system)
        (assoc-in entity [:component/weapon :weapon/last-attack-timestamp] (now))))))

(s/fdef process-firing-entities
  :args (s/cat :entities (s/coll-of :entity/entity))
  :ret (s/coll-of :entity/entity))

;; System definition

(def attack-system
  {:system/tick-fn process-firing-entities})
