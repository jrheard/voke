(ns voke.world.visualize
  (:require [cljs.spec :as s]
            [reagent.core :as r]
            [voke.world.generation :as generate]))

(s/def ::active-cell ::generate/cell)
(s/def ::dungeon (s/keys :req [::generate/grid ::active-cell]))

(def cell-size 15)

; TODO construct some sort of system that takes a ::generation/world and draws its progress over time

(let [world (-> (generate/full-grid 30 30)
                (generate/drunkards-walk 150))]
  (def dungeon (r/atom {::generate/grid (world ::generate/grid)
                        ::active-cell   [2 2]})))

(defn row [a-row y]
  [:div.row
   (for [[x cell] (map-indexed vector a-row)]
     ^{:key ["cell" x y]} [:div.cell {:class (name cell)}])])

; TODO rewrite when grid is 1d
(defn grid [dungeon]
  [:div.world
   (conj (for [[y a-row] (map-indexed vector (@dungeon ::generate/grid))]
           ^{:key ["row" y]} [row a-row y])

         (let [[x y] (@dungeon ::active-cell)]
           [:div.cell.active {:style {:left (* cell-size x)
                                      :top  (* cell-size y)}}]))])


(defn ^:export main []
  (r/render-component [grid dungeon]
                      (js/document.getElementById "content")))


(comment
  (reset! dungeon
          (-> (generate/full-grid 30 30)
              (generate/drunkards-walk 200)
              )
          )

  )
