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

(def run-systems (make-system-running-fn))
