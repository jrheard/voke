(ns voke.world.visualize
  (:require [reagent.core :as r]
            [voke.util :refer [timeout]]
            [voke.world.generation :as generate]))

;; Constants

(def grid-width 100)
(def grid-height 100)
(def canvas-height 800)
(def canvas-width 800)

; TODO revisit *all* of these atoms

(defonce selected-tab (r/atom :drunkard))

(defonce rng-seed (atom (.valueOf (js/Date.))))

; drunkard's
(defonce num-empty-cells (r/atom 400))

; automata
(defonce initial-fill-chance (r/atom 0.45))
(defonce first-pass-survival-threshold (r/atom 4))
(defonce first-pass-birth-threshold (r/atom 5))
(defonce smoothing-pass-survival-threshold (r/atom 4))
(defonce smoothing-pass-birth-threshold (r/atom 5))
(defonce num-iterations (r/atom 10000))
(defonce smoothing-passes (r/atom 0))

(defn reset-rng-seed! []
  (reset! rng-seed (.valueOf (js/Date.))))

;; Canvas manipulation

(defn get-ctx []
  (-> "visualization-canvas"
      (js/document.getElementById)
      (.getContext "2d")))

(defn draw-grid
  [grid]
  (let [ctx (get-ctx)
        width (count (first grid))
        height (count grid)
        cell-width (/ canvas-width width)
        cell-height (/ canvas-height height)]

    (.clearRect ctx 0 0 canvas-width canvas-height)
    (set! (.-fillStyle ctx) "#CCC")

    (loop [x 0 y 0]

      (when (< y height)
        (when (= (-> grid
                     (get y)
                     (get x))
                 :empty)

          (doto ctx
            (.beginPath)
            (.rect (* x cell-width) (* y cell-height) cell-width cell-height)
            (.fill)))

        (recur (if (identical? (dec x) width) 0 (inc x))
               (if (identical? (dec x) width) (inc y) y))))))

(defn generate-grid-and-draw []
  (Math/seedrandom (str @rng-seed))
  (let [grid
        (condp = @selected-tab
          :drunkard (generate/drunkards-walk grid-width grid-height @num-empty-cells)
          :automata (generate/automata grid-width
                                       grid-height
                                       @initial-fill-chance
                                       @first-pass-survival-threshold
                                       @first-pass-birth-threshold
                                       @num-iterations
                                       @smoothing-passes
                                       @smoothing-pass-survival-threshold
                                       @smoothing-pass-birth-threshold)
          :final (generate/automata 200 200 0.45 4 5 400000 12 4 5))]
    (draw-grid grid)))

;; Reagent components

(defn slider
  [an-atom min max step]
  [:input {:type      "range" :value @an-atom :min min :max max :step step
           :style     {:width "100%"}
           :on-change (fn [e]
                        (reset! an-atom (js/parseFloat (.-target.value e)))
                        (generate-grid-and-draw))}])

(defn tab
  [tab-kw text]
  [:a.btn.pill {:href     "#"
                :class    (when (= @selected-tab tab-kw)
                            "selected")
                :on-click (fn [e]
                            (.preventDefault e)
                            (reset! selected-tab tab-kw)
                            (generate-grid-and-draw))}
   text])

(defn ui []
  [:div.content
   [:p "Algorithm:"]
   [:div.btn-group
    [tab :drunkard "Drunkard's Walk"]
    [tab :automata "Cellular Automata"]
    [tab :final "Voke algorithm"]]

   (when (= @selected-tab :drunkard)
     [:div.drunkard-specific
      [:p (str "Dig until there are " @num-empty-cells " empty cells in the grid")]
      [slider num-empty-cells 0 2000 10]])

   (when (= @selected-tab :automata)
     [:div.cellular-specific
      [:p (str "Chance for a given cell to be filled during intialization pass: " @initial-fill-chance)]
      [slider initial-fill-chance 0 1 0.01]
      [:p (str "Mininum # of neighbors for an alive cell to survive (first pass): " @first-pass-survival-threshold)]
      [slider first-pass-survival-threshold 0 8 1]
      [:p (str "Mininum # of neighbors for a cell to be born (first pass): " @first-pass-birth-threshold)]
      [slider first-pass-birth-threshold 0 8 1]
      [:p (str "Number of times to apply automata rules to random individual cells: " @num-iterations)]
      [slider num-iterations 0 40000 5000]
      [:p (str "Number of smoothing passes: " @smoothing-passes)]
      [slider smoothing-passes 0 12 1]
      [:p (str "Mininum # of neighbors for an alive cell to survive (smoothing pass): " @smoothing-pass-survival-threshold)]
      [slider smoothing-pass-survival-threshold 0 8 1]
      [:p (str "Mininum # of neighbors for a cell to be born (smoothing pass): " @smoothing-pass-birth-threshold)]
      [slider smoothing-pass-birth-threshold 0 8 1]])

   [:div.button-wrapper
    [:a.generate-button
     {:href     "#"
      :on-click (fn [e]
                  (.preventDefault e)
                  (reset-rng-seed!)
                  (generate-grid-and-draw))}
     "generate a new one"]]

   [:canvas {:id     "visualization-canvas"
             :width  canvas-width
             :height canvas-height
             :style  {:border           "none"
                      :background-color "#333"}}]])

;; Main

(defn ^:export main []
  (r/render-component [ui]
                      (js/document.getElementById "content"))
  (generate-grid-and-draw))
