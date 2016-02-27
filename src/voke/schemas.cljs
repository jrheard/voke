(ns voke.schemas
  (:require [schema.core :as s])
  (:require-macros [schema.core :as sm]))

(sm/defschema Direction (s/enum :up :right :down :left))
(sm/defschema IntendedDirection #{Direction})

(sm/defschema EntityField (s/enum :position
                                  :collision-box
                                  :render-info
                                  :human-controlled
                                  :intended-move-direction
                                  :intended-fire-direction))

; TODO i guess i should read up on the strategy pattern if i want to use this term
(sm/defschema MovementStrategy {:type (s/enum :human-controlled
                                              :skeleton
                                              :projectile)})

(sm/defschema Position {:x s/Num
                        :y s/Num})

(sm/defschema Entity {:id                                       s/Int
                      (s/optional-key :position)                Position
                      (s/optional-key :collision-box)           {:width  s/Int
                                                                 :height s/Int}
                      (s/optional-key :render-info)             {:shape (s/enum :square)}
                      (s/optional-key :human-controlled)        s/Bool
                      ; TODO - replace the line above with the line below
                      (s/optional-key :movement-strategy)       MovementStrategy
                      (s/optional-key :indended-move-direction) IntendedDirection
                      (s/optional-key :intended-fire-direction) IntendedDirection})

(sm/defschema GameState {:entities {:s/Int Entity}})

(sm/defschema EventType (s/enum :movement :update-entity))

; TODO unused / not well defined
(sm/defschema Event {:type EventType
                     s/Any s/Any})

(sm/defschema System {(s/optional-key :every-tick)     {(s/optional-key :reads) #{EntityField}
                                                        :fn                     s/Any}
                      (s/optional-key :event-handlers) [{:event-type EventType
                                                         :fn         s/Any}]})
