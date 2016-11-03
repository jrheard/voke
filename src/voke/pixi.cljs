(ns voke.pixi
  (:require [cljs.spec :as s]
            [cljsjs.pixi]))

(def viewport-width 1000)
(def viewport-height 700)

(defn make-renderer
  [width height node]
  (doto (js/PIXI.autoDetectRenderer
          width
          height
          #js {:view node})
    (aset "backgroundColor" 0xFFFFFF)))

(defonce renderer (make-renderer viewport-width viewport-height (js/document.getElementById "screen")))
(defonce stage (js/PIXI.Container.))
(.render renderer stage)

(defonce -graphics-pool
         (atom
           (vec
             (for [_ (range 200)]
               (js/PIXI.Graphics.)))))

(defn acquire-graphics []
  (let [graphics (peek @-graphics-pool)]
    (assert graphics)
    (swap! -graphics-pool pop)
    graphics))

(defn release-graphics [graphics]
  (.clear graphics)
  (swap! -graphics-pool conj graphics))


(defonce graphics-by-entity-id (atom {}))

(defn rectangle
  [x y w h color]
  (let [graphics (doto (acquire-graphics)
                   (.beginFill color)
                   (.drawRect 0 0 w h)
                   (.endFill))]
    (doto (aget graphics "position")
      (aset "x" (- x (/ w 2)))
      (aset "y" (- y (/ h 2))))
    (.addChild stage graphics)
    graphics))

(defn entity->graphics!
  [entity]
  (rectangle (-> entity :component/shape :shape/center :geometry/x)
             (-> entity :component/shape :shape/center :geometry/y)
             (-> entity :component/shape :shape/width)
             (-> entity :component/shape :shape/height)
             (-> entity :component/render :render/fill)))

(s/fdef entity->graphics!
  :args (s/cat :entity :entity/entity))

(defn handle-unknown-entities! [entities]
  ; TODO - only actually operate on the entity if it's visible
  (doseq [entity entities]
    (swap! graphics-by-entity-id
           assoc
           (:entity/id entity)
           (entity->graphics! entity))))

(defn update-entity-position!
  [entity-id new-center]
  (when-let [graphics (@graphics-by-entity-id entity-id)]
    (let [shape (-> graphics
                    (aget "graphicsData")
                    (aget 0)
                    (aget "shape"))
          position (aget graphics "position")]
      (aset position "x" (- (new-center :geometry/x) (/ (aget shape "width") 2)))
      (aset position "y" (- (new-center :geometry/y) (/ (aget shape "height") 2))))))

(s/fdef update-entity-position!
  :args (s/cat :entity-id :entity/id
               :new-center :geometry/vector2))

(defn remove-entity!
  [entity-id]
  (let [graphics (@graphics-by-entity-id entity-id)]
    (.removeChild stage graphics)
    (release-graphics graphics)))

(defn update-camera-position!
  [center-x center-y]
  (aset stage "x" (- (/ viewport-width 2) center-x))
  (aset stage "y" (- (/ viewport-height 2) center-y)))

(defn render! [entities]
  (handle-unknown-entities! (filter
                              #(not (contains? @graphics-by-entity-id (:entity/id %)))
                              entities))
  ; TODO update entity visibility
  (.render renderer stage))
