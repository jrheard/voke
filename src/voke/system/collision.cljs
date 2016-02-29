(ns voke.system.collision
  (:require [voke.events :refer [publish-event]]
            [voke.schemas :refer [Event]])
  (:require-macros [schema.core :as sm]))

(sm/defn handle-intended-movement
  [event :- Event
   publish-chan]
  (js/console.log (clj->js event))
  )

(def collision-system
  {:event-handlers [{:event-type :intended-movement
                     :fn         handle-intended-movement}]})
