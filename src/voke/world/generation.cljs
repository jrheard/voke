(ns voke.world.generation
  (:require [cljs.spec :as s]
            [cljs.spec.test :as stest]
            [taoensso.tufte :as tufte :refer-macros [p profiled profile]]
            [voke.util :refer [bound-between rand-nth-weighted]]))

(s/def ::cell #{:empty :full})
(s/def ::width nat-int?)
(s/def ::height nat-int?)
(s/def ::grid (s/coll-of (s/coll-of ::cell)))

(defn full-grid [w h]
  (vec (repeat h
               (vec (repeat w :full)))))

(defn ^:export drunkards-walk [w h num-empty-cells]
  (loop [grid (full-grid w h)
         x (rand-int w)
         y (rand-int h)
         empty-cells 0]

    (if (identical? empty-cells num-empty-cells)
      grid

      (let [cell-was-full? (= (get-in grid [y x]) :full)
            direction (rand-nth [:north :south :east :west])]

        (recur (assoc-in grid [y x] :empty)
               (case direction
                 :east (bound-between (inc x) 0 (dec w))
                 :west (bound-between (dec x) 0 (dec w))
                 x)
               (case direction
                 :north (bound-between (dec y) 0 (dec h))
                 :south (bound-between (inc y) 0 (dec h))
                 y)
               (if cell-was-full?
                 (inc empty-cells)
                 empty-cells))))))

(s/fdef drunkards-walk
  :args (s/cat :w nat-int?
               :h nat-int?
               :num-empty-cells nat-int?)
  :ret ::grid)

;; Cellular automata

(defn array->grid [an-array]
  (into []
        (map (fn [row]
               (into []
                     (map (fn [cell]
                            (if (true? cell) :full :empty)))
                     row)))
        an-array))

(defn -make-js-row [width full-probability]
  (let [arr (make-array width)]
    (loop [i 0]
      (when (< i width)
        (aset arr i (if (< (rand) full-probability)
                      true
                      false))
        (recur (inc i))))
    arr))

(defn -make-js-grid [width height full-probability]
  (let [arr (make-array height)]
    (loop [i 0]
      (when (< i height)
        (aset arr i (-make-js-row width full-probability))
        (recur (inc i))))
    arr))

(defn -num-full-neighbors [js-grid x y w h]
  (loop [num-full 0
         i (dec x)
         j (dec y)]
    (if (and (< i (+ x 2))
             (< j (+ y 2)))

      (let [this-cell-counts? (cond
                                (and (identical? i x)
                                     (identical? j y)) false

                                ; Off-grid cells count as filled-in neighbors.
                                (or (< i 0)
                                    (>= i w)
                                    (< j 0)
                                    (>= j h)) true

                                :else (-> js-grid
                                          (aget j)
                                          (aget i)))]

        (recur (if this-cell-counts?
                 (inc num-full)
                 num-full)
               (if (identical? i (inc x))
                 (dec x)
                 (inc i))
               (if (identical? i (inc x))
                 (inc j)
                 j)))

      num-full)))

(defn -new-value-at-position-inner [^boolean cell-was-full ^boolean meets-survival-threshold ^boolean meets-birth-treshold]
  "See blog.fikesfarm.com/posts/2015-06-15-see-js-in-cljs-repl.html"
  (or
    (and cell-was-full meets-survival-threshold)
    (and (not cell-was-full) meets-birth-treshold)))

(defn -new-value-at-position
  [js-grid x y w h survival-threshold birth-threshold]
  (let [cell-is-full? (-> js-grid
                          (aget y)
                          (aget x))
        num-full-neighbors (-num-full-neighbors js-grid x y w h)]
    (-new-value-at-position-inner cell-is-full?
                                  (>= num-full-neighbors survival-threshold)
                                  (>= num-full-neighbors birth-threshold))))

(defn -copy-js-grid [js-grid]
  (let [height (.-length js-grid)
        new-grid (make-array height)]

    (loop [i 0]
      (if (identical? i height)
        new-grid

        (do
          (aset new-grid
                i
                (.slice (aget js-grid i)))
          (recur (inc i)))))))

(defn -automata-smoothing-pass
  [js-grid w h survival-threshold birth-threshold]
  (let [new-grid (-copy-js-grid js-grid)]
    (loop [x 0
           y 0]
      (when (< y h)
        (-> new-grid
            (aget y)
            (aset x (-new-value-at-position js-grid x y w h survival-threshold birth-threshold)))
        (recur (if (identical? (inc x) w) 0 (inc x))
               (if (identical? (inc x) w) (inc y) y))))
    new-grid))

(defn -run-automata-rules-on-random-individual-cells
  [js-grid w h survival-threshold birth-threshold iterations]
  (loop [i 0]
    (when-not (identical? i iterations)
      (let [x (rand-int w)
            y (rand-int h)]
        (-> js-grid
            (aget y)
            (aset x (-new-value-at-position js-grid x y w h survival-threshold birth-threshold)))
        (recur (inc i))))))

(defn ^:export automata
  [w h initial-wall-probability first-pass-survival-threshold first-pass-birth-threshold
   iterations smoothing-passes smoothing-pass-survival-threshold smoothing-pass-birth-threshold]
  (let [js-grid (-make-js-grid w h initial-wall-probability)
        _ (-run-automata-rules-on-random-individual-cells
            js-grid w h first-pass-survival-threshold first-pass-birth-threshold iterations)

        smoothed-js-grid (loop [i 0
                                grid js-grid]
                           (if (identical? i smoothing-passes)
                             grid
                             (recur (inc i)
                                    (-automata-smoothing-pass
                                      grid w h smoothing-pass-survival-threshold smoothing-pass-birth-threshold))))]

    (array->grid smoothed-js-grid)))

(comment
  (tufte/add-basic-println-handler! {})

  (profile
    {}
    (let [grid (full-grid 30 30)]
      (dotimes [_ 100]
        (p :drunkard
           (drunkards-walk grid 150)
           nil))))

  (profile
    {}
    (dotimes [_ 400]
      (p :automata
         (automata 100 100 0.45 3 4 20000 12 4 5)
         nil)))
  )

