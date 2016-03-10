(ns voke.system.attack
  (:require [plumbing.core :refer [safe-get-in]]
            [schema.core :as s]
            [voke.entity :refer [projectile]]
            [voke.schemas :refer [Axis Entity System]]
            [voke.util :refer [now]])
  (:require-macros [schema.core :as sm]))

(def directions-to-velocity-multipliers {:down  {:y 5}
                                         :up    {:y -5}
                                         :left  {:x -5}
                                         :right {:x 5}})

(def maximum-entity-velocity-shot-speed-contribution 2)

(sm/defn can-attack? :- s/Bool
  [entity :- Entity]
  ; TODO support monsters
  (and
    (seq (get-in entity [:brain :intended-fire-direction]))
    (> (- (now)
          (get-in entity [:weapon :last-attack-timestamp]))
       ; TODO parameterize on :weapon
       150)))

(sm/defn entity-velocity-contribution
  [entity :- Entity
   axis :- Axis]
  (let [axis-velocity (safe-get-in entity [:motion :velocity axis])]
    (if (pos? axis-velocity)
      (min maximum-entity-velocity-shot-speed-contribution axis-velocity)
      (max (- maximum-entity-velocity-shot-speed-contribution) axis-velocity))))

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
         ; TODO update these values
         (projectile (entity :id)
                     (get-in entity [:shape :x])
                     (get-in entity [:shape :y])
                     (get-in entity [:shape :orientation])
                     10
                     10
                     ; TODO jesus christ cap velocity
                     x-velocity
                     y-velocity)]))))

;; System definition

(sm/def attack-system :- System
  {:every-tick {:fn process-firing-entities}})
