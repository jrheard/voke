(ns voke.world.visualize
  (:require [cljs.spec :as s]
            [cljs.core.async :refer [chan <! put!]]
            [reagent.core :as r]
            [voke.util :refer [timeout]]
            [voke.world.generation :as generate])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

;; Constants

(def cell-size 15)
(defonce ms-per-tick (r/atom 16))
(defonce grid-width (r/atom 30))
(defonce grid-height (r/atom 30))
(defonce num-empty-cells (r/atom 100))

(defonce visualization-state (r/atom {::generate/grid (generate/full-grid @grid-width @grid-height)
                                      ::active-cell   nil
                                      ::id            0}))

(defn reset-visualization-state!
  ([] (reset-visualization-state! (generate/full-grid @grid-width @grid-height)))
  ([initial-grid]
   (swap! visualization-state (fn [old-state]
                                {::generate/grid initial-grid
                                 ::active-cell   nil
                                 ::id            (inc (old-state ::id))}))))

;; Async code

(defn animate-dungeon-history [generated-level]
  (let [initial-grid (or (generated-level ::generate/initial-grid)
                         (generate/full-grid @grid-width @grid-height))
        visualization-id ((reset-visualization-state! initial-grid) ::id)]

    (go-loop [history (generated-level ::generate/history)]
      (when (and (seq history)
                 (= (@visualization-state ::id) visualization-id))
        (<! (timeout @ms-per-tick))

        (let [[[x y] new-value] (first history)]
          (swap! visualization-state (fn [state]
                                       (if (= (state ::id) visualization-id)
                                         (-> state
                                             (assoc-in [::generate/grid y x] new-value)
                                             (assoc ::active-cell [x y]))
                                         state))))
        (recur (rest history))))))

(s/fdef animate-dungeon-history
  :args (s/cat :historical-active-cells ::generate/history
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

(defn slider [an-atom min max callback]
  (let [_ @an-atom]
    [:input {:type      "range" :value @an-atom :min min :max max
             :style     {:width "100%"}
             :on-change (fn [e]
                          (reset! an-atom (js/parseInt (.-target.value e)))
                          (callback))}]))

(defn ui [visualization-state]
  [:div.content
   [:p (str "Grid width: " @grid-width)]
   [slider grid-width 25 50 reset-visualization-state!]
   [:p (str "Grid height: " @grid-height)]
   [slider grid-height 25 50 reset-visualization-state!]
   [:p (str "Dig until there are " @num-empty-cells " empty cells in the grid")]
   [:p "(doesn't take effect until the next time you press \"generate\")"]
   [slider num-empty-cells 10 300]
   [:p (str "Animation speed: " @ms-per-tick " ms per frame")]
   [slider ms-per-tick 5 250]
   [:div.button-wrapper
    [:a.generate-button
     {:href     "#"
      :on-click (fn [e]
                  (.preventDefault e)
                  (let [new-dungeon (generate/automata @grid-width @grid-height 0.45 5000)]
                    #_(animate-dungeon-history new-dungeon)

                    (reset-visualization-state! (new-dungeon ::generate/grid))
                    ))}
     "generate"]]
   [grid visualization-state]])

;; Main

(defn ^:export main []
  (r/render-component [ui visualization-state]
                      (js/document.getElementById "content")))
