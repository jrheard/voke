(ns voke.system.ai.system
  (:require [voke.schemas :refer [System]])
  (:require-macros [schema.core :as sm]))

(sm/def ai-system :- System
  {:tick-fn (fn [entities]
              (let [player (->> entities
                                (filter #(contains? % :input))
                                first)
                    player-center (get-in player [:shape :center])
                    entities-with-ai (filter #(contains? % :ai) entities)]

                (for [entity entities-with-ai]
                  (let [entity-center (get-in entity [:shape :center])]
                    (assoc-in entity
                              [:motion :direction]
                              (Math/atan2 (- (player-center :y)
                                             (entity-center :y))
                                          (- (player-center :x)
                                             (entity-center :x))))))))})
