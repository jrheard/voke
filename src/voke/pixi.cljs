(ns voke.pixi
  (:require [cljsjs.pixi]
            [voke.schemas :refer [Entity Shape System]])
  (:require-macros [schema.core :as sm]))

(defn rectangle [graphic x y w h color]
  (doto graphic
    (.beginFill color)
    (.drawRect 0 0 w h)
    (.endFill)
    (aset "x" (- x (/ w 2)))
    (aset "y" (- y (/ h 2)))))

(sm/defn entity->graphic
  ; FIXME only supports rectangles atm, doesn't look to see if you've got other shapes
  [entity :- Entity]
  (rectangle (js/PIXI.Graphics.)
             (-> entity :shape :center :x)
             (-> entity :shape :center :y)
             (-> entity :shape :width)
             (-> entity :shape :height)
             0x333333))

(sm/defn update-obj-position!
  [obj
   shape :- Shape]
  ; xxx also only supports rectangles
  (doto obj
    (aset "x" (- (get-in shape [:center :x]) (/ (shape :width) 2)))
    (aset "y" (- (get-in shape [:center :y]) (/ (shape :height) 2)))))

(defn make-renderer [width height node]
  (doto (js/PIXI.CanvasRenderer.
          width
          height
          #js {:view node})
    (aset "backgroundColor" 0xFFFFFF)))

(defn make-stage []
  (js/PIXI.Container.))

; TODO codify "obj" language/terminology
(defn add-to-stage! [stage obj]
  (.addChild stage obj))

(defn remove-from-stage! [stage obj]
  (.removeChild stage obj))

(defn render! [renderer stage]
  (.render renderer stage))
