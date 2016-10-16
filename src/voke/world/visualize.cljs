(ns voke.world.visualize
  (:require [cljs.spec :as s]
            [reagent.core :as r]
            [voke.world.generation :as generate]))

; TODO spec out generation state, figure out how it evoles over time
; construct some sort of system that takes a ::generation/world and draws its progress over time

(defonce generation-state (-> (generate/full-grid 20 20)
                              (generate/drunkards-walk 100)
                              r/atom))

(defn row [a-row row-num]
  [:div.row
   (for [[i cell] (map-indexed vector a-row)]
     ^{:key ["cell" row-num i]} [:div.cell {:class (name cell)}])])

; TODO rewrite when grid is 1d
(defn grid [generation-state]
  [:div.world
   (for [[i a-row] (map-indexed vector (::generate/grid @generation-state))]
     ^{:key ["row" i]} [row a-row i])])


(defn ^:export main []
  (r/render-component [grid generation-state]
                      (js/document.getElementById "content")))


(comment
  (reset! generation-state
          (-> (generate/full-grid 30 30)
              (generate/drunkards-walk 200)
              )
          )

  )
