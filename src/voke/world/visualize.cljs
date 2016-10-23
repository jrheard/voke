(ns voke.world.visualize
  (:require [cljs.spec :as s]
            [cljs.core.async :refer [chan <! put!]]
            [reagent.core :as r]
            [voke.util :refer [timeout]]
            [voke.world.generation :as generate])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

; This is a tool I built to help me play around with and understand various random-level-generation
; algorithms. The code in this file is messy/gross/bad because it isn't "production" code, just
; "throwaway dev tool" code. Don't judge me :)


;; Constants

(def cell-size 10)

(defonce selected-tab (r/atom :drunkard))
(defonce ms-per-tick (r/atom 16))
(defonce grid-width (r/atom 80))
(defonce grid-height (r/atom 80))

; drunkard's
(defonce num-empty-cells (r/atom 400))

; automata
(defonce initial-fill-chance (r/atom 0.45))
(defonce min-neighbors-to-survive (r/atom 4))
(defonce min-neighbors-to-birth (r/atom 5))
(defonce num-iterations (r/atom 10000))
(defonce smoothing-passes (r/atom 0))

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

(defn draw-automata-grid! []
  (let [new-dungeon (generate/automata @grid-width
                                       @grid-height
                                       @initial-fill-chance
                                       @min-neighbors-to-survive
                                       @min-neighbors-to-birth
                                       @num-iterations
                                       @smoothing-passes)]
    (swap! visualization-state (fn [state]
                                 (-> state
                                     (assoc ::generate/grid (new-dungeon ::generate/grid))
                                     (assoc ::active-cell nil))))))


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

(defn grid [visualization-state]
  [:div.world
   (conj (for [[y a-row] (map-indexed vector (@visualization-state ::generate/grid))]
           ^{:key ["row" y]} [row a-row y])

         (when-let [[x y] (@visualization-state ::active-cell)]
           ^{:key "active-cell"} [:div.cell.active {:style {:left (* cell-size x)
                                                            :top  (* cell-size y)}}]))])

(defn slider [an-atom min max step callback]
  (let [_ @an-atom]
    [:input {:type      "range" :value @an-atom :min min :max max :step step
             :style     {:width "100%"}
             :on-change (fn [e]
                          (reset! an-atom (js/parseFloat (.-target.value e)))
                          (when callback
                            (callback)))}]))

(defn common-visualization-sliders []
  [:div.common-sliders
   [:p (str "Grid width: " @grid-width)]
   [slider grid-width 25 100 1 reset-visualization-state!]
   [:p (str "Grid height: " @grid-height)]
   [slider grid-height 25 100 1 reset-visualization-state!]
   [:p (str "Animation speed: " @ms-per-tick " ms per frame")]
   [slider ms-per-tick 16 250 1]])

(defn tab
  [tab-kw text]
  [:a.btn.pill {:href     "#"
                :class    (when (= @selected-tab tab-kw)
                            "selected")
                :on-click (fn [e]
                            (.preventDefault e)
                            (reset! selected-tab tab-kw)
                            (reset-visualization-state!))}
   text])

(defn ui [visualization-state]
  [:div.content
   [:p "Algorithm:"]
   [:div.btn-group
    [tab :drunkard "Drunkard's Walk"]
    [tab :cellular "Cellular Automata"]
    [tab :final "Voke algorithm"]]

   (when (not= @selected-tab :final)
     (common-visualization-sliders))

   (when (= @selected-tab :drunkard)
     [:div.drunkard-specific
      [:p (str "Dig until there are " @num-empty-cells " empty cells in the grid")]
      [:p "(doesn't take effect until the next time you press \"generate\")"]
      [slider num-empty-cells 10 1000 1]])

   (when (= @selected-tab :cellular)
     [:div.cellular-specific
      [:p (str "Chance for a given cell to be filled during intialization pass: "
               @initial-fill-chance)]
      [slider initial-fill-chance 0 1 0.01 draw-automata-grid!]
      [:p (str "Mininum # of neighbors for an alive cell to survive: " @min-neighbors-to-survive)]
      [slider min-neighbors-to-survive 0 8 1 draw-automata-grid!]
      [:p (str "Mininum # of neighbors for a cell to be born: " @min-neighbors-to-birth)]
      [slider min-neighbors-to-birth 0 8 1 draw-automata-grid!]
      [:p (str "Number of times to apply automata rules to random individual cells: " @num-iterations)]
      [slider num-iterations 0 40000 5000 draw-automata-grid!]
      [:p (str "Number of smoothing passes: " @smoothing-passes)]
      [slider smoothing-passes 0 5 1 draw-automata-grid!]
      ])

   [:div.button-wrapper
    [:a.generate-button
     {:href     "#"
      :on-click (fn [e]
                  (.preventDefault e)
                  (condp = @selected-tab
                    :drunkard (animate-dungeon-history
                                (generate/drunkards-walk @grid-width @grid-height @num-empty-cells))
                    :automata (draw-automata-grid!)
                    :final (let [canvas (js/document.getElementById "visualization-canvas")
                                 ctx (.getContext canvas "2d")
                                 side-length 400
                                 cell-size 2
                                 grid ((generate/automata side-length side-length 0.45 4 5 400000 12)
                                        ::generate/grid)]
                             (.clearRect ctx 0 0 (* side-length cell-size) (* side-length cell-size))

                             (set! (.-fillStyle ctx) "#CCC")

                             (loop [x 0
                                    y 0]

                               (when (< y side-length)
                                 (when (= (-> grid
                                              (get y)
                                              (get x))
                                          :empty)

                                   (doto ctx
                                     (.beginPath)
                                     (.rect (* x cell-size) (* y cell-size) cell-size cell-size)
                                     (.fill)))

                                 (recur (if (= (dec x) side-length) 0 (inc x))
                                        (if (= (dec x) side-length) (inc y) y)))))))}
     "generate"]]

   ; full #333, empty #ccc

   (if (= @selected-tab :final)
     [:canvas {:id     "visualization-canvas"
               :width  800
               :height 800
               :style  {:border           "none"
                        :background-color "#333"}
               }]
     [grid visualization-state])])

;; Main

(defn ^:export main []
  (r/render-component [ui visualization-state]
                      (js/document.getElementById "content")))
