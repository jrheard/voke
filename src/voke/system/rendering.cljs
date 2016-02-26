(ns voke.system.rendering
  (:require [voke.schemas :refer [Entity]])
  (:require-macros [schema.core :as sm]))

(sm/defn render-system
  [entities :- [Entity]]
  ; TODO  get a good canvas library via cljsjs, fabric or phaser seem to be top contenders, research
  (let [canvas (js/document.getElementById "screen")
        ctx (.getContext canvas "2d")]
    (.clearRect ctx
                0
                0
                (.-width canvas)
                (.-height canvas))
    (aset ctx "fillStyle" "rgb(50,50,50)")
    (doseq [entity entities]
      (.fillRect ctx
                 (-> entity :position :x)
                 (-> entity :position :y)
                 (-> entity :collision-box :width)
                 (-> entity :collision-box :height)))))

(defn a-rendering-event-handler [event]
  (js/console.log (clj->js event)))
