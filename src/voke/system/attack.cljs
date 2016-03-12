(ns voke.system.attack
  (:require [plumbing.core :refer [safe-get-in]]
            [schema.core :as s]
            [voke.entity :refer [projectile]]
            [voke.schemas :refer [Axis Entity System]]
            [voke.util :refer [bound-between now]])
  (:require-macros [schema.core :as sm]))

(def directions-to-velocity-multipliers {:down  {:y 5}
                                         :up    {:y -5}
                                         :left  {:x -5}
                                         :right {:x 5}})

(def maximum-entity-velocity-shot-speed-contribution 2)

;; everything in here is gross and that is ok for now

(sm/defn can-attack? :- s/Bool
  [entity :- Entity]
  ; TODO support monsters
  (and
    (seq (get-in entity [:brain :intended-fire-direction]))
    (> (- (now)
          (get-in entity [:weapon :last-attack-timestamp]))
       ; TODO parameterize on :weapon
       25)))

(sm/defn entity-velocity-contribution
  [entity :- Entity
   axis :- Axis]
  (bound-between (safe-get-in entity [:motion :velocity axis])
                 (- maximum-entity-velocity-shot-speed-contribution)
                 maximum-entity-velocity-shot-speed-contribution))

(sm/defn process-firing-entities :- [Entity]
  [entities :- [Entity]]
  (flatten
    (for [entity (filter can-attack? entities)]
      (let [direction (last (safe-get-in entity [:brain :intended-fire-direction]))
            ; multiplier will eventually be used with a :shot-speed component that'll live somewhere on :weapon
            multiplier (directions-to-velocity-multipliers direction)
            x-velocity (+ (entity-velocity-contribution entity :x)
                          (get multiplier :x 0))
            y-velocity (+ (entity-velocity-contribution entity :y)
                          (get multiplier :y 0))]
        ; TODO - consider having projectiles start right at the border of their parent entity, instead of inside
        [(assoc-in entity
                   [:weapon :last-attack-timestamp]
                   (now))
         (projectile (entity :id)
                     (safe-get-in entity [:shape :center])
                     (safe-get-in entity [:weapon :projectile-shape])
                     (safe-get-in entity [:shape :orientation])
                     x-velocity
                     y-velocity)]))))

;; System definition

(sm/def attack-system :- System
  {:tick-fn process-firing-entities})
