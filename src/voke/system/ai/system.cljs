(ns voke.system.ai.system
  (:require [voke.schemas :refer [System]])
  (:require-macros [schema.core :as sm]))

(sm/def ai-system :- System
  {:tick-fn (fn [entities]
              (let [player (->> entities
                                (filter #(contains? % :input))
                                first)
                    entities-with-ai (filter #(contains? % :ai) entities)]
                (for [entity entities-with-ai]
                  entity
                  )
                )
              )})
