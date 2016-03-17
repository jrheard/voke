(ns voke.system.attack
  (:require [plumbing.core :refer [safe-get-in]]
            [schema.core :as s]
            [voke.entity :refer [projectile]]
            [voke.schemas :refer [Axis Direction Entity System]]
            [voke.state :refer [add-entity!]]
            [voke.util :refer [bound-between now]])
  (:require-macros [schema.core :as sm]))

(def maximum-entity-velocity-shot-speed-contribution 2)

;; everything in here is gross and that is ok for now

(sm/defn can-attack? :- s/Bool
  [entity :- Entity]
  (let [weapon (entity :weapon)]
    (and
      (get-in entity [:weapon :fire-direction])
      (> (- (now)
            (get-in entity [:weapon :last-attack-timestamp]))
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

(sm/defn process-firing-entities :- [Entity]
  [entities :- [Entity]]
  (flatten
    (for [entity (filter can-attack? entities)]
      (let [collides-with (hash-set
                            :obstacle
                            :item
                            (if (= (get-in entity [:collision :type]) :good-guy)
                              :bad-guy
                              :good-guy))]
        ; TODO - consider having projectiles start right at the border of their parent entity, instead of inside
        (add-entity! (projectile (entity :id)
                                 (safe-get-in entity [:shape :center])
                                 collides-with
                                 (safe-get-in entity [:weapon :projectile-shape])
                                 (safe-get-in entity [:weapon :projectile-color])
                                 (safe-get-in entity [:shape :orientation])
                                 (shot-speed entity :x)
                                 (shot-speed entity :y))
                     :attack-system)
        (assoc-in entity
                  [:weapon :last-attack-timestamp]
                  (now))))))

;; System definition

(sm/def attack-system :- System
  {:tick-fn process-firing-entities})
