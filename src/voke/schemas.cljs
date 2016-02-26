(ns voke.schemas
  (:require [schema.core :as s])
  (:require-macros [schema.core :as sm]))

(sm/defschema Direction (s/enum :up :right :down :left))
(sm/defschema IntendedDirection #{Direction})

(sm/defschema Entity {:id                                s/Int
                      (s/maybe :position)                {:x s/Num
                                                          :y s/Num}
                      (s/maybe :collision-box)           {:width  s/Int
                                                          :height s/Int}
                      (s/maybe :render-info)             {:shape (s/enum :square)}
                      (s/maybe :human-controlled)        s/Bool
                      (s/maybe :indended-move-direction) IntendedDirection
                      (s/maybe :intended-fire-direction) IntendedDirection})

; TODO perhaps a map by entity id
(sm/defschema GameState {:entities [Entity]})
