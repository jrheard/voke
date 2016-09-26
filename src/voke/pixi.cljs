(ns voke.pixi
  (:require [cljsjs.pixi]
            [voke.schemas :refer [Entity Shape System]])
  (:require-macros [schema.core :as sm]))

(defn make-renderer
  [width height node]
  (doto (js/PIXI.CanvasRenderer.
          width
          height
          #js {:view node})
    (aset "backgroundColor" 0xFFFFFF)))

(defn make-stage
  [graphics]
  (let [stage (js/PIXI.Container.)]
    (.addChild stage graphics)
    stage))

(defonce renderer (make-renderer 1000 700 (js/document.getElementById "screen")))
(defonce graphics (js/PIXI.Graphics.))
(defonce stage (make-stage graphics))

(defonce graphics-data-by-entity-id (atom {}))

; TODO borders
(defn rectangle
  [x y w h color]
  (doto graphics
    (.beginFill color)
    (.drawRect 0 0 w h)
    (.endFill))
  (let [graphics-data-list (aget graphics "graphicsData")
        graphics-data (aget graphics-data-list
                            (- (.-length graphics-data-list)
                               1))]
    (doto (aget graphics-data "shape")
      (aset "x" (- x (/ w 2)))
      (aset "y" (- y (/ h 2))))
    graphics-data))

(sm/defn entity->graphics-data!
  [entity :- Entity]
  (rectangle (-> entity :shape :center :x)
             (-> entity :shape :center :y)
             (-> entity :shape :width)
             (-> entity :shape :height)
             (-> entity :render-info :fill)))

(defn handle-unknown-entities! [entities]
  ; TODO - only actually operate on the entity if it's visible
  (doseq [entity entities]
    (swap! graphics-data-by-entity-id
           assoc
           (:id entity)
           (entity->graphics-data! entity))))

(sm/defn update-entity-position!
  [entity-id new-center]
  (let [graphics-data (@graphics-data-by-entity-id entity-id)]
    (when graphics-data
      (let [shape (aget graphics-data "shape")]
        (aset shape "x" (- (new-center :x) (/ (aget shape "width") 2)))
        (aset shape "y" (- (new-center :y) (/ (aget shape "height") 2)))))))

(defn remove-entity!
  [entity-id]
  (let [graphics-data (@graphics-data-by-entity-id entity-id)
        graphics-data-list (aget graphics "graphicsData")
        index (.indexOf graphics-data-list graphics-data)]

    (when (> index -1)
      (.splice graphics-data-list index 1)
      (.destroy graphics-data))))

(defn render! [entities]
  (handle-unknown-entities! (filter
                              #(not (contains? @graphics-data-by-entity-id (:id %)))
                              entities))
  ; TODO update entity visibility
  ; TODO something something camera
  (.render renderer stage))
