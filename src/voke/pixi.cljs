(ns voke.pixi
  (:require [cljs.spec :as s]
            [cljsjs.pixi]))

(def viewport-width 1000)
(def viewport-height 700)

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

(defonce renderer (make-renderer viewport-width viewport-height (js/document.getElementById "screen")))
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

(defn entity->graphics-data!
  [entity]
  (rectangle (-> entity :component/shape :shape/center :geometry/x)
             (-> entity :component/shape :shape/center :geometry/y)
             (-> entity :component/shape :shape/width)
             (-> entity :component/shape :shape/height)
             (-> entity :component/render :render/fill)))

(s/fdef entity->graphics-data!
  :args (s/cat :entity :entity/entity))

(defn handle-unknown-entities! [entities]
  ; TODO - only actually operate on the entity if it's visible
  (doseq [entity entities]
    (swap! graphics-data-by-entity-id
           assoc
           (:entity/id entity)
           (entity->graphics-data! entity))))

(defn update-entity-position!
  [entity-id new-center]
  (let [graphics-data (@graphics-data-by-entity-id entity-id)]
    (when graphics-data
      (let [shape (aget graphics-data "shape")]
        (aset shape "x" (- (new-center :geometry/x) (/ (aget shape "width") 2)))
        (aset shape "y" (- (new-center :geometry/y) (/ (aget shape "height") 2)))))))

(s/fdef update-entity-position!
  :args (s/cat :entity-id :entity/id
               :new-center :geometry/vector2))

(defn remove-entity!
  [entity-id]
  (let [graphics-data (@graphics-data-by-entity-id entity-id)
        graphics-data-list (aget graphics "graphicsData")
        index (.indexOf graphics-data-list graphics-data)]

    (when (> index -1)
      (.splice graphics-data-list index 1)
      (.destroy graphics-data))))

(defn update-camera-position!
  [center-x center-y]
  (aset stage "x" (- (/ viewport-width 2) center-x))
  (aset stage "y" (- (/ viewport-height 2) center-y)))

(defn render! [entities]
  (handle-unknown-entities! (filter
                              #(not (contains? @graphics-data-by-entity-id (:entity/id %)))
                              entities))
  ; TODO update entity visibility
  (.render renderer stage))
