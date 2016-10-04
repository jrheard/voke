(ns voke.events
  "A super-simple synchronous pub/sub system."
  (:require [cljs.spec :as s]))

;; Specs

(s/def :event/type #{:movement :entity-added :entity-removed :contact})
(s/def :event/event (s/keys :req [:event/type]))

; A map of {:event/type -> [event-handler-fn]}.
(def ^:private registry (atom {}))

;; Public

(defn publish-event [event]
  (doseq [handler (@registry (event :event/type))]
    (handler event)))

(s/fdef publish-event
  :args (s/cat :event :event/event))

(defn subscribe-to-event
  [event-type handler-fn]
  (swap! registry
         update-in
         [event-type]
         (fn [handlers]
           (if (seq handlers)
             (conj handlers handler-fn)
             [handler-fn]))))
(s/fdef subscribe-to-event
  :args (s/cat :event-type :event/type
               :handler-fn fn?))

(defn unsub-all! []
  (reset! registry {}))
