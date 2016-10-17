(ns voke.world.visualize
  (:require [cljs.spec :as s]
            [cljs.core.async :refer [chan <! put!]]
            [reagent.core :as r]
            [voke.util :refer [timeout]]
            [voke.world.generation :as generate])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(s/def ::active-cell ::generate/cell)
(s/def ::dungeon (s/keys :req [::generate/grid ::active-cell]))

(def cell-size 15)
(def ms-per-tick 50)

; TODO construct some sort of system that takes a ::generation/world and draws its progress over time

(defn make-dungeon []
  {::generate/grid (generate/full-grid 30 30)
   ::active-cell   nil})

(defonce dungeon (r/atom (make-dungeon)))

(defn animate-dungeon-history [historical-active-cells w h]
  (reset! dungeon {::generate/grid (generate/full-grid w h)
                   ::active-cell   nil})

  (go-loop [history historical-active-cells]
    (when (seq history)
      (<! (timeout ms-per-tick))

      (let [[x y] (first history)]
        (swap! dungeon (fn [a-dungeon]
                         (-> a-dungeon
                             (assoc-in [::generate/grid y x] :empty)
                             (assoc ::active-cell [x y])))))
      (recur (rest history)))))

(s/fdef animate-dungeon-history
  :args (s/cat :historical-active-cells ::generate/historical-active-cells
               :w nat-int?
               :h nat-int?))

(defn row [a-row y]
  [:div.row
   (for [[x cell] (map-indexed vector a-row)]
     ^{:key ["cell" x y]} [:div.cell {:class (name cell)}])])

; TODO rewrite when grid is 1d
(defn grid [dungeon]
  [:div.world
   (conj (for [[y a-row] (map-indexed vector (@dungeon ::generate/grid))]
           ^{:key ["row" y]} [row a-row y])

         (when-let [[x y] (@dungeon ::active-cell)]
           ^{:key "active-cell"} [:div.cell.active {:style {:left (* cell-size x)
                                                            :top  (* cell-size y)}}]))])

(defn ui [dungeon]
  [:div.content
   ^{:key "dungeon"} [grid dungeon]
   ^{:key "button"} [:button {:on-click (fn [e]
                                          (.preventDefault e)
                                          (let [new-dungeon (-> (generate/full-grid 30 30)
                                                                (generate/drunkards-walk 100))]
                                            (animate-dungeon-history (new-dungeon ::generate/historical-active-cells)
                                                                     30
                                                                     30)))}
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
