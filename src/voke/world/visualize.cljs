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

(def grid-width 100)
(def grid-height 100)
(def canvas-height 800)
(def canvas-width 800)
(def ms-per-tick 16)

; TODO revisit *all* of these atoms
; consolidate them and reconcile them with this "visualization state" atom

(defonce selected-tab (r/atom :drunkard))

; drunkard's
(defonce num-empty-cells (r/atom 400))

; automata
(defonce initial-fill-chance (r/atom 0.45))
(defonce min-neighbors-to-survive (r/atom 4))
(defonce min-neighbors-to-birth (r/atom 5))
(defonce num-iterations (r/atom 10000))
(defonce smoothing-passes (r/atom 0))

(defonce visualization-state (r/atom {::generate/grid (generate/full-grid grid-width grid-height)
                                      ::active-cell   nil
                                      ::id            0}))

(defn reset-visualization-state!
  ([] (reset-visualization-state! (generate/full-grid grid-width grid-height)))
  ([initial-grid]
   (swap! visualization-state (fn [old-state]
                                {::generate/grid initial-grid
                                 ::active-cell   nil
                                 ::id            (inc (old-state ::id))}))))

;; Async code

(defn animate-dungeon-history [generated-level]
  ; useless for now, voke.world.generation needs to be updated to pass along complete histories
  (let [initial-grid (or (generated-level ::generate/initial-grid)
                         (generate/full-grid grid-width grid-height))
        visualization-id ((reset-visualization-state! initial-grid) ::id)]

    (go-loop [history (generated-level ::generate/history)]
      (when (and (seq history)
                 (= (@visualization-state ::id) visualization-id))
        (<! (timeout ms-per-tick))

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

        (recur (if (= (dec x) width) 0 (inc x))
               (if (= (dec x) width) (inc y) y))))))

(defn draw-automata-grid! []
  (-> (generate/automata grid-width
                         grid-height
                         @initial-fill-chance
                         @min-neighbors-to-survive
                         @min-neighbors-to-birth
                         @num-iterations
                         @smoothing-passes)
      ::generate/grid
      draw-grid))

;; Reagent components

(defn slider [an-atom min max step callback]
  (let [_ @an-atom]
    [:input {:type      "range" :value @an-atom :min min :max max :step step
             :style     {:width "100%"}
             :on-change (fn [e]
                          (reset! an-atom (js/parseFloat (.-target.value e)))
                          (when callback
                            (callback)))}]))

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
    [tab :automata "Cellular Automata"]
    [tab :final "Voke algorithm"]]

   (when (= @selected-tab :drunkard)
     [:div.drunkard-specific
      [:p (str "Dig until there are " @num-empty-cells " empty cells in the grid")]
      [slider num-empty-cells 0 1000 50
       #(draw-grid ((generate/drunkards-walk grid-width grid-height @num-empty-cells)
                     ::generate/grid))]])

   (when (= @selected-tab :automata)
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
      [slider smoothing-passes 0 12 1 draw-automata-grid!]])

   [:div.button-wrapper
    [:a.generate-button
     {:href     "#"
      :on-click (fn [e]
                  (.preventDefault e)
                  (condp = @selected-tab
                    :drunkard (draw-grid ((generate/drunkards-walk grid-width grid-height @num-empty-cells)
                                           ::generate/grid))
                    :automata (draw-automata-grid!)
                    :final (draw-grid ((generate/automata 400 400 0.45 4 5 400000 12)
                                        ::generate/grid))))}
     "generate"]]

   [:canvas {:id     "visualization-canvas"
             :width  canvas-width
             :height canvas-height
             :style  {:border           "none"
                      :background-color "#333"}}]])

;; Main

(defn ^:export main []
  (r/render-component [ui visualization-state]
                      (js/document.getElementById "content")))
