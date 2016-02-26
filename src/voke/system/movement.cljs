(ns voke.system.movement
  (:require [voke.events :refer [publish-event]]
            [voke.schemas :refer [Entity GameState System]])
  (:require-macros [schema.core :as sm]))

(def direction-value-mappings {:up    {:y -1}
                               :down  {:y 1}
                               :left  {:x -1}
                               :right {:x 1}})

(sm/defn apply-intended-movement-directions :- Entity
  [entity :- Entity]
  (loop [directions (entity :intended-move-direction)
         entity entity]
    (if (seq directions)
      (let [direction (first directions)
            [axis value] (first (direction-value-mappings direction))]
        (recur (rest directions)
               (update-in entity [:position axis] + (* 5 value))))
      entity)))

(sm/def move-system :- System
  {:every-tick {:reads #{:intended-move-direction}
                :fn    (fn move-system-tick [entities publish-chan]
                         (for [entity entities]
                           (let [moved-entity (apply-intended-movement-directions entity)]
                             (publish-event publish-chan {:event-type :movement :entity :moved-entity})
                             moved-entity)))}})
