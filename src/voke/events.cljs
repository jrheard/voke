(ns voke.events
  "A super-simple pub/sub system."
  (:require [voke.schemas :refer [EventType]])
  (:require-macros [schema.core :as sm]))

(def ^:private registry (atom {}))

(sm/defn publish-event [event]
  ;(js/console.log (clj->js (event :event-type)))
  ;(js/console.log (clj->js event))
  (doseq [handler (@registry (event :event-type))]
    (handler event)))

(sm/defn subscribe-to-event
  [event-type :- EventType
   handler-fn]
  (swap! registry
         update-in
         [event-type]
         (fn [handlers]
           (if (seq handlers)
             (conj handlers handler-fn)
             [handler-fn]))))

(defn unsub-all! []
  (reset! registry {}))
