(ns voke.events
  "A simple pub/sub system.

  TODO REIMPLEMENT AS A SIMPLE SYNCHRONOUS SYSTEM THAT DOESN'T USE CORE.ASYNC
  SEE dev-diary.txt

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
  ;(js/console.log (clj->js event))
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
