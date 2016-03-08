(ns voke.schemas
  (:require [schema.core :as s])
  (:require-macros [schema.core :as sm]))

(sm/defschema EntityID s/Int)

(sm/defschema Direction (s/enum :up :right :down :left))
(sm/defschema IntendedDirection #{Direction})

(sm/defschema Shape {:x           s/Num
                     :y           s/Num
                     :type        (s/enum :rectangle :circle)
                     :orientation s/Num                     ; Orientation in radians
                     s/Any        s/Any})

(sm/defschema Motion {:velocity             {:x s/Num
                                             :y s/Num}
                      :affected-by-friction s/Bool
                      :max-speed            s/Num
                      :max-acceleration     s/Num})

(sm/defschema Weapon {:last-attack-timestamp s/Int})

(sm/defschema Brain {:type                    (s/enum :player :skeleton :projectile)
                     ; TODO - these will be *angles* for non-player entities, not sets!!!!!
                     :intended-move-direction IntendedDirection
                     :intended-fire-direction IntendedDirection})

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
                      ; TODO - give me a single example of an entity that doesn't have a shape
                      (s/optional-key :shape)      Shape
                      (s/optional-key :motion)     Motion
                      ; If it doesn't have a :collision, the entity isn't collidable.
                      (s/optional-key :collision)  Collision
                      (s/optional-key :renderable) s/Bool   ; TODO this will have like colors and stuff
                      (s/optional-key :owner-id)   EntityID
                      (s/optional-key :weapon)     Weapon
                      (s/optional-key :brain)      Brain})

(sm/defschema GameState {:entities {:s/Int Entity}})

(sm/defschema EventType (s/enum :movement :update-entity :intended-movement))

; TODO not well defined
; maybe best thing to do would be to schematize each individual event and then say an Event is any of 'em
(sm/defschema Event {:event-type EventType
                     s/Any       s/Any})

; TODO i reallly think i should kill this :reads field
(sm/defschema System {(s/optional-key :every-tick)     {(s/optional-key :reads) #{s/Keyword}
                                                        :fn                     s/Any}
                      (s/optional-key :event-handlers) [{:event-type EventType
                                                         :fn         s/Any}]})
