(ns voke.events
  "A super-simple synchronous pub/sub system."
  (:require [voke.schemas :refer [EventType]])
  (:require-macros [schema.core :as sm]))

; A map of {event-name -> [event-handler-fn]}.
(def ^:private registry (atom {}))
; TODO rename :event-type to :type

(sm/defn publish-event [event]
  (doseq [handler (@registry (event :type))]
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
