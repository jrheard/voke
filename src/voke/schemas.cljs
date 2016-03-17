(ns voke.schemas
  (:require [schema.core :as s])
  (:require-macros [schema.core :as sm]))

(sm/defschema EntityID s/Int)

(sm/defschema IntendedDirection (s/enum :up :right :down :left))
(sm/defschema IntendedDirectionSet #{IntendedDirection})

(sm/defschema Axis (s/enum :x :y))

(sm/defschema Vector2 {:x s/Num
                       :y s/Num})

(sm/defschema Shape {:type        (s/enum :rectangle :circle)
                     :center      Vector2
                     :orientation s/Num                     ; Orientation in radians
                     s/Any        s/Any})

(defn maybe-0-2pi
  [x]
  (or
    (nil? x)
    (< 0 x (* Math/PI 2))))

; urp seems to go from neg 2pi to 2pi
(def Direction maybe-0-2pi)

(sm/defschema ProjectileShape (dissoc Shape :orientation :center))

(sm/defschema Motion {:velocity             Vector2
                      :direction            Direction
                      :affected-by-friction s/Bool
                      :max-speed            s/Num
                      :max-acceleration     s/Num})

(sm/defschema Weapon {:last-attack-timestamp s/Int
                      :fire-direction        Direction
                      :shots-per-second      s/Num
                      :projectile-shape      ProjectileShape})

(sm/defschema Input {:intended-move-direction IntendedDirectionSet
                     :intended-fire-direction IntendedDirectionSet})

(sm/defschema CollisionType (s/enum
                              :good-guy
                              :bad-guy
                              :projectile
                              :obstacle
                              :trap
                              :projectile
                              :gold
                              :item
                              :explosion))

(sm/defschema Collision {:type                                  CollisionType
                         (s/optional-key :collides-with)        #{CollisionType}
                         (s/optional-key :destroyed-on-contact) s/Bool})

(sm/defschema Entity {:id                          EntityID
                      :shape                       Shape
                      (s/optional-key :motion)     Motion
                      ; If it doesn't have a :collision, the entity isn't collidable.
                      (s/optional-key :collision)  Collision
                      (s/optional-key :renderable) s/Bool   ; TODO this will have like colors and stuff
                      (s/optional-key :owner-id)   EntityID
                      (s/optional-key :weapon)     Weapon
                      (s/optional-key :input)      Input})

(sm/defschema GameState {:entities {:s/Int Entity}})

(sm/defschema EventType (s/enum :movement :entity-added :entity-removed))

; TODO not well defined
; maybe best thing to do would be to schematize each individual event and then say an Event is any of 'em
(sm/defschema Event {:type EventType
                     s/Any s/Any})

(sm/defschema System {(s/optional-key :tick-fn)        s/Any
                      (s/optional-key :initialize)     s/Any
                      (s/optional-key :event-handlers) [{:event-type EventType
                                                         :fn         s/Any}]})
