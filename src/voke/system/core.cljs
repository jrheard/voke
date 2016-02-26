(ns voke.system.core
  (:require [voke.events :refer [make-pub subscribe-to-event]]
            [voke.schemas :refer [GameState]]
            [voke.system.movement :refer [move-system]]
            [voke.system.rendering :refer [render-system a-rendering-event-handler]])
  (:require-macros [schema.core :as sm]))

(sm/defn make-system-running-fn []
  (let [{:keys [publish-chan publication]} (make-pub)]
    (subscribe-to-event publication :movement a-rendering-event-handler)

    (fn [state]
      (render-system (:entities state))
      (-> state
          (move-system publish-chan)))))

; TODO turn systems into maps
; let the movement system say that i'ts only interested in entities that have an intended move direction
; in particular - this is interesting - it's only interested in entities that have a _non-empty list_ of
; intended move directions

(def run-systems (make-system-running-fn))
