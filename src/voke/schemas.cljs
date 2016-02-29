(ns voke.schemas
  (:require [schema.core :as s])
  (:require-macros [schema.core :as sm]))

(sm/defschema Direction (s/enum :up :right :down :left))
(sm/defschema IntendedDirection #{Direction})

(sm/defschema EntityField (s/enum :shape
                                  :collision
                                  :renderable
                                  :human-controlled
                                  :intended-move-direction
                                  :intended-fire-direction))

; TODO i guess i should read up on the strategy pattern if i want to use this term
(sm/defschema MovementStrategy {:type (s/enum :human-controlled
                                              :skeleton
                                              :projectile)})

; TODO - x/y should be the *center*, not the topleft
(sm/defschema Shape {:x     s/Num
                     :y     s/Num
                     :type  (s/enum :rectangle :circle)
                     :angle s/Num                           ; Orientation in radians
                     s/Any  s/Any})

(sm/defschema Entity {:id                                       s/Int
                      (s/optional-key :shape)                   Shape
                      ; If it doesn't have a :collision-info, the entity isn't collidable.
                      (s/optional-key :collision-info)          {:type (s/enum
                                                                         :player
                                                                         :projectile
                                                                         :monster
                                                                         :obstacle
                                                                         :level-end)}
                      (s/optional-key :renderable)              s/Bool ; TODO this will have like colors and stuff
                      (s/optional-key :human-controlled)        s/Bool
                      ; TODO - replace the line above with the line below
                      (s/optional-key :movement-strategy)       MovementStrategy
                      (s/optional-key :indended-move-direction) IntendedDirection
                      (s/optional-key :intended-fire-direction) IntendedDirection})

(sm/defschema GameState {:entities {:s/Int Entity}})

(sm/defschema EventType (s/enum :movement :update-entity))

; TODO not well defined
(sm/defschema Event {:event-type EventType
                     s/Any       s/Any})

(sm/defschema System {(s/optional-key :every-tick)     {(s/optional-key :reads) #{EntityField}
                                                        :fn                     s/Any}
                      (s/optional-key :event-handlers) [{:event-type EventType
                                                         :fn         s/Any}]})
