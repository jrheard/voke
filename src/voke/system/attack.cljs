(ns voke.system.attack
  (:require [schema.core :as s]
            [voke.entity :refer [projectile]]
            [voke.events :refer [publish-event]]
            [voke.schemas :refer [Entity System]]
            [voke.util :refer [now]])
  (:require-macros [schema.core :as sm]))

(sm/defn can-attack? :- s/Bool
  [entity :- Entity]
  ; TODO support monsters
  (and
    (seq (get-in entity [:brain :intended-fire-direction]))
    (> (- (now)
          (get-in entity [:weapon :last-attack-timestamp]))
       50)))

(sm/defn process-firing-entities :- [Entity]
  ; NOTE - only called on things that intend to fire
  [entities :- [Entity]
   _]
  (flatten
    (for [entity (filter can-attack? entities)]
      ; for each entity that can attack

      ; update the entity's weapon's last-attack-timestamp

      ; return a projectile moving in the right direction (based on :intended-fire-direction)

      ; ok so really we just want to build up a list of updated entities and new projectiles, and return them




      ; TODO - consider having projectiles start right at the border of their parent entity, instead of inside

      [(assoc-in entity
                   [:weapon :last-attack-timestamp]
                   (now))
         (projectile (entity :id)
                     (get-in entity [:shape :x])
                     (get-in entity [:shape :y])
                     (get-in entity [:shape :orientation])
                     10
                     10
                     1
                     1)])))

;; System definition

(sm/def attack-system :- System
  {:every-tick {:fn process-firing-entities}})
