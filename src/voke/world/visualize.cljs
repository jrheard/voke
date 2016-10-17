(ns voke.world.visualize
  (:require [cljs.spec :as s]
            [cljs.core.async :refer [chan <! put!]]
            [reagent.core :as r]
            [voke.util :refer [timeout]]
            [voke.world.generation :as generate])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

;; Constants

(def cell-size 15)
(def ms-per-tick 50)
(def grid-width 30)
(def grid-height 30)

(defonce visualization-state (r/atom {::generate/grid (generate/full-grid grid-width grid-height)
                                      ::active-cell   nil
                                      ::id            0}))

(defn reset-visualization-state [old-state]
  {::generate/grid (generate/full-grid grid-width grid-height)
   ::active-cell   nil
   ::id            (inc (old-state ::id))})

;; Async code

(defn animate-dungeon-history [historical-active-cells w h]
  (let [visualization-id (-> visualization-state
                             (swap! reset-visualization-state)
                             ::id)]

    (go-loop [history historical-active-cells]
      (when (and (seq history)
                 (= (@visualization-state ::id) visualization-id))
        (<! (timeout ms-per-tick))

        (let [[x y] (first history)]
          (swap! visualization-state (fn [state]
                                       (if (= (state ::id) visualization-id)
                                         (-> state
                                             (assoc-in [::generate/grid y x] :empty)
                                             (assoc ::active-cell [x y]))
                                         state))))
        (recur (rest history))))))

(s/fdef animate-dungeon-history
  :args (s/cat :historical-active-cells ::generate/historical-active-cells
               :w nat-int?
               :h nat-int?))

;; Reagent components

(defn row [a-row y]
  [:div.row
   (for [[x cell] (map-indexed vector a-row)]
     ^{:key ["cell" x y]} [:div.cell {:class (name cell)}])])

; TODO rewrite when grid is 1d
(defn grid [visualization-state]
  [:div.world
   (conj (for [[y a-row] (map-indexed vector (@visualization-state ::generate/grid))]
           ^{:key ["row" y]} [row a-row y])

         (when-let [[x y] (@visualization-state ::active-cell)]
           ^{:key "active-cell"} [:div.cell.active {:style {:left (* cell-size x)
                                                            :top  (* cell-size y)}}]))])

(defn ui [visualization-state]
  [:div.content
   ^{:key "dungeon"} [grid visualization-state]
   ^{:key "button"} [:button {:on-click (fn [e]
                                          (.preventDefault e)
                                          (let [new-dungeon (-> (generate/full-grid 30 30)
                                                                (generate/drunkards-walk 100))]
                                            (animate-dungeon-history (new-dungeon ::generate/historical-active-cells)
                                                                     30
                                                                     30)))}
                     "generate"]])

;; Main

(defn ^:export main []
  (r/render-component [ui visualization-state]
                      (js/document.getElementById "content")))


(comment
  (reset! dungeon
          (-> (generate/full-grid 30 30)
              (generate/drunkards-walk 200)
              )
          )

  )
