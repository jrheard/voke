(ns voke.system.rendering
  (:require [voke.schemas :refer [Entity System]])
  (:require-macros [schema.core :as sm]))

(sm/defn render-entity
  [ctx
   entity :- Entity]
  ; TODO  get a good canvas library via cljsjs, fabric or phaser seem to be top contenders, research
  (aset ctx "fillStyle" "rgb(50,50,50)")
  (.fillRect ctx
             (-> entity :position :x)
             (-> entity :position :y)
             (-> entity :collision-box :width)
             (-> entity :collision-box :height)))

(defn a-rendering-event-handler [event]
  (js/console.log (clj->js event)))

(sm/def render-system :- System
  {:every-tick {:reads #{:position :render-info}
                :fn    (fn render-system-tick [entities publish-chan]
                         ; TODO this will become a no-op when pixi happens
                         ; nvm! we *will* need to perform a side effect every tick
                         (let [canvas (js/document.getElementById "screen")
                               ctx (.getContext canvas "2d")]
                           (.clearRect ctx
                                       0
                                       0
                                       (.-width canvas)
                                       (.-height canvas))

                           (doseq [entity entities]
                             (render-entity ctx entity)))
                         entities)}})

