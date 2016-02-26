(ns voke.system.rendering
  (:require [cljsjs.pixi]
            [voke.schemas :refer [Entity System]])
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

(sm/defn render-system-tick [renderer stage entities publish-chan]
  (.render renderer stage)
  entities)

(defn a-rendering-event-handler [event]
  ; TODO event has an :entity
  (js/console.log (clj->js event)))

; TODO does this defonce fuck us figwheel-wise? irritating if so
; gonna need to rethink this defonce.
; gonna need to put some thought into how to make this whole system reloadable in general
(defonce render-system
         (let [renderer (js/PIXI.autoDetectRenderer. 800 600)
               stage (js/PIXI.Container.)]
           (.appendChild js/document.body (.-view renderer))

           {:every-tick {:reads #{:position :render-info}
                         :fn    (fn [& args]
                                  (apply render-system-tick renderer stage args))}
            :event-handlers [{:event-type :movement
                              :fn a-rendering-event-handler}]}))
