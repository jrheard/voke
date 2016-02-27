(ns voke.pixi
  (:require [cljsjs.pixi]
            [voke.schemas :refer [Entity Position System]])
  (:require-macros [schema.core :as sm]))

(sm/defn entity->graphic
  ; FIXME only supports rectangles atm, doesn't look to see if you've got other shapes
  [entity :- Entity]
  (doto (js/PIXI.Graphics.)
    (.beginFill 0xEEEEEE)
    (.drawRect 0
               0
               ; TODO i'm not in love with how this system uses :collision-boxes.
               ; should :rendering-info include a dupe :width/:height?
               ; both options suck (sniping :collision-box's data vs duplicating data)
               (-> entity :collision-box :width)
               (-> entity :collision-box :height))
    (.endFill)
    (aset "x" (-> entity :position :x))
    (aset "y" (-> entity :position :y))))

(sm/defn update-obj-position!
  [obj
   new-position :- Position]
  (doto obj
    (aset "x" (new-position :x))
    (aset "y" (new-position :y))))

(defn make-renderer [width height node]
  (js/PIXI.autoDetectRenderer.
                   width
                   height
                   #js {:view node}))

(defn make-stage []
  (js/PIXI.Container.))

; TODO codify "obj" language/terminology
(defn add-to-stage! [stage obj]
  (.addChild stage obj))

(defn render! [renderer stage]
  (.render renderer stage))
