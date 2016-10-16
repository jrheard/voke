(ns voke.world.visualize
  (:require [cljs.spec :as s]
            [cljs.core.async :refer [chan <! put!]]
            [reagent.core :as r]
            [voke.world.generation :as generate])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(s/def ::active-cell ::generate/cell)
(s/def ::dungeon (s/keys :req [::generate/grid ::active-cell]))

(def cell-size 15)

; TODO construct some sort of system that takes a ::generation/world and draws its progress over time

(defn make-dungeon []

  (let [world (-> (generate/full-grid 30 30)
                  (generate/drunkards-walk 150))]
    {::generate/grid (world ::generate/grid)
     ::active-cell   [2 2]}))

(defonce dungeon (r/atom (make-dungeon)))

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
           ^{:key "active-cell"} [:div.cell.active {:style {:left (* cell-size x)
                                                            :top  (* cell-size y)}}]))])

(defn ui [dungeon]
  [:div.content
   ^{:key "dungeon"} [grid dungeon]
   ^{:key "button"} [:button {:on-click (fn [e]
                                          (.preventDefault e)
                                          (reset! dungeon (make-dungeon)))}
                     "generate"]])

(defn ^:export main []
  (r/render-component [ui dungeon]
                      (js/document.getElementById "content")))


(comment
  (reset! dungeon
          (-> (generate/full-grid 30 30)
              (generate/drunkards-walk 200)
              )
          )

  )
