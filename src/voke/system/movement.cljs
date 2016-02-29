(ns voke.system.movement
  (:require [voke.events :refer [publish-event]]
            [voke.schemas :refer [Entity GameState System]])
  (:require-macros [schema.core :as sm]))

(def direction-value-mappings {:up    {:y -1}
                               :down  {:y 1}
                               :left  {:x -1}
                               :right {:x 1}})

(sm/defn apply-intended-movement-directions :- Entity
  "Takes an Entity with some :intended-move-directions and updates the entity's
  :position to reflect its intention to move in those directions."
  ; TODO above comment will be out of date when collision system is implemneted
  [entity :- Entity]
  (loop [directions (entity :intended-move-direction)
         entity entity]
    ; TODO velocity / acceleration
    (if (seq directions)
      (let [direction (first directions)
            [axis value] (first (direction-value-mappings direction))]
        (recur (rest directions)
               (update-in entity [:shape axis] + (* 5 value))))
      entity)))

;;; System definition

(sm/def move-system :- System
  {:every-tick {:fn (fn move-system-tick [entities publish-chan]
                      (doseq [entity (filter #(not-empty (:intended-move-direction %)) entities)]
                        (publish-event publish-chan {:event-type   :intended-movement
                                                     :moved-entity (apply-intended-movement-directions entity)
                                                     :all-entities entities}))
                      entities)}})
