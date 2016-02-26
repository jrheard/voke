(ns voke.system.movement
  (:require [voke.events :refer [publish-event]]
            [voke.schemas :refer [GameState]])
  (:require-macros [schema.core :as sm]))

(def direction-value-mappings {:up    {:y -1}
                               :down  {:y 1}
                               :left  {:x -1}
                               :right {:x 1}})

(sm/defn move-system :- GameState
  [state :- GameState
   publish-chan]
  (publish-event publish-chan {:event-type :movement :foo :bar})
  (update-in state [:entities] #(mapv (fn [entity]
                                        (loop [directions (entity :intended-move-direction)
                                               entity entity]
                                          (if (seq directions)
                                            (let [direction (first directions)
                                                  [axis value] (first (direction-value-mappings direction))]
                                              (recur (rest directions)
                                                     (update-in entity [:position axis] + (* 5 value))))
                                            entity)))
                                      %)))
