(ns voke.events
  "A simple pub/sub system.

  See https://yobriefca.se/blog/2014/06/04/publish-and-subscribe-with-core-dot-asyncs-pub-and-sub"
  (:require [cljs.core.async :refer [chan <! put! pub sub]]
            [voke.schemas :refer [EventType]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [schema.core :as sm]))

(defn make-pub []
  (let [publish-chan (chan)]
    {:publish-chan publish-chan
     :publication (pub publish-chan :event-type)}))

(sm/defn publish-event [publish-chan event]
  ; TODO consider making `event-type` be an arg that's assoc'd onto the given event
  ; this would prevent dumb bugs like the time i had an event with a :type rather than an :event-type
  (put! publish-chan event))

(sm/defn subscribe-to-event
  [publication
   event-type :- EventType
   handler-fn]
  (let [subscriber (chan)]
    (sub publication event-type subscriber)
    (go-loop []
      (handler-fn (<! subscriber))
      (recur))))
