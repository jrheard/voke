(ns voke.events
  (:require [cljs.core.async :refer [chan <! >! put! pub sub]]
            [voke.schemas :refer [EventType]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [schema.core :as sm]))

(defn make-pub []
  ; context: https://yobriefca.se/blog/2014/06/04/publish-and-subscribe-with-core-dot-asyncs-pub-and-sub/
  ; TODO document
  (let [publish-chan (chan)]
    {:publish-chan publish-chan
     :publication (pub publish-chan :event-type)}))

(sm/defn publish-event [publish-chan event]
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

; ok are events primarily for side effects?
; otherwise like how do they affect the state of the game
; you can't just pass a global mutable game-state atom around, that's no good
; so for now let's just say that events are solely for side effects - updating the rendering
; system, playing a sound, whatever - and that `every-tick` functions are where the bulk
; of the game's work gets done, because they're pure
