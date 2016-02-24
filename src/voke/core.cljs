(ns voke.core
  (:require [schema.core :as s]
            [goog.events :as events])
  (:import [goog.events KeyCodes])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [schema.core :as sm])
  (:use [cljs.core.async :only [chan <! >! put! timeout]]))

(sm/defschema Entity {:id                      s/Int
                      (s/maybe :position)      {:x s/Num
                                                :y s/Num}
                      (s/maybe :collision-box) {:width  s/Int
                                                :height s/Int}
                      (s/maybe :render-info)   {:shape (s/enum :square)}})

(def player {:id            1
             :position      {:x 10
                             :y 10}
             :collision-box {:width 50 :height 50}
             :render-info   {:shape :square}})

; TODO :mode? :active-level?
(def game-state (atom {:entities [player]}))

(sm/defn render-system
  [entities :- [Entity]]
  (js/console.log (clj->js entities))
  (let [canvas (js/document.getElementById "screen")
        ctx (.getContext canvas "2d")]
    (doseq [entity entities]
      (aset ctx "fillStyle" "rgb(50,50,50)")
      (.fillRect ctx
                 (-> entity :position :x)
                 (-> entity :position :y)
                 (-> entity :collision-box :width)
                 (-> entity :collision-box :height)
                 ))
    )
  )

(defn ^:export main []
  (render-system (:entities @game-state))
  )



(comment
  @app-state

  )
