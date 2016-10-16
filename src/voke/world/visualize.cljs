(ns voke.world.visualize
  (:require [cljs.spec :as s]
            [reagent.core :as r]
            [voke.world.generation :as generate]))

(defonce world (-> (generate/full-grid 10 10)
                   (generate/drunkards-walk 10)
                   r/atom))

(defn row [a-row row-num]
  [:div.row
   (for [[i cell] (map-indexed vector a-row)]
     ^{:key ["cell" row-num i]} [:div.cell {:class (name cell)}])])

; TODO rewrite when grid is 1d
(defn grid [world]
  [:div.world
   (for [[i a-row] (map-indexed vector (::generate/grid @world))]
     ^{:key ["row" i]} [row a-row i])])


(defn ^:export main []
  (r/render-component [grid world]
                      (js/document.getElementById "content")))


(comment
  (-> (generate/full-grid 5 5)
      (generate/drunkards-walk 5)
      ::generate/grid
      ;grid
      )

  )
