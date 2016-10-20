(ns voke.world.generation
  (:require [cljs.spec :as s]
            [cljs.spec.test :as stest]
            [taoensso.tufte :as tufte :refer-macros [p profiled profile]]
            [voke.util :refer [bound-between rand-nth-weighted]]))

(s/def ::cell #{:empty :full})
(s/def ::width nat-int?)
(s/def ::height nat-int?)
(s/def ::grid (s/coll-of (s/coll-of ::cell)))

(s/def ::vector2 (s/cat :x nat-int? :y nat-int?))
(s/def ::history (s/cat :position ::vector2 :new-value ::cell))
(s/def ::generated-level (s/keys :req [::grid ::history]))

(defn full-grid [w h]
  (vec (repeat h
               (vec (repeat w :full)))))

(defn ^:export drunkards-walk [w h num-empty-cells]
  (loop [grid (full-grid w h)
         historical-active-cells []
         x (rand-int w)
         y (rand-int h)
         empty-cells 0]

    (if (= empty-cells num-empty-cells)
      {::grid    grid
       ::history historical-active-cells}

      (let [cell-was-full? (= (get-in grid [y x]) :full)
            horizontal-direction-to-center (if (< x (/ w 2)) :east :west)
            vertical-direction-to-center (if (< y (/ h 2)) :south :north)
            direction (rand-nth-weighted
                        (into {}
                              (map (fn [direction]
                                     (if (#{horizontal-direction-to-center vertical-direction-to-center}
                                           direction)
                                       [direction 1.2]
                                       [direction 1.0]))
                                   [:north :south :east :west])))]

        (recur (assoc-in grid [y x] :empty)
               (conj historical-active-cells [[x y] :empty])
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
  :ret ::generated-level)

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

(defn -get-neighbors [js-grid x y w h]
  (let [neighbors #js []]
    (loop [i (dec x)
           j (dec y)]
      (when (and (< i (+ x 2))
                 (< j (+ y 2)))

        (cond
          (and (identical? i x)
               (identical? j y)) nil

          (or (< i 0)
              (>= i w)
              (< j 0)
              (>= j h)) (.push neighbors true)

          :else (.push neighbors (-> js-grid
                                     (aget j)
                                     (aget i))))

        (if (identical? i (inc x))
          (recur (dec x)
                 (inc j))
          (recur (inc i)
                 j))))

    neighbors))

(defn -new-value-at-position
  [js-grid x y w h survival-threshold birth-threshold]
  (let [cell-is-full? (-> js-grid
                          (aget y)
                          (aget x))
        neighbors (-get-neighbors js-grid x y w h)
        num-full-neighbors (.-length (.filter neighbors true?))]

    (cond
      (and cell-is-full?
           (> num-full-neighbors survival-threshold)) true
      (and (not cell-is-full?)
           (> num-full-neighbors birth-threshold)) true
      :else false)))

(defn -copy-js-grid [js-grid]
  (let [height (.-length js-grid)
        new-grid (make-array height)]

    (loop [i 0]
      (if (= i height)
        new-grid

        (aset new-grid
              i
              (.slice (aget js-grid i)))))))

(defn -automata-smoothing-pass
  [js-grid w h survival-threshold birth-threshold]
  (let [new-grid (-copy-js-grid js-grid)]
    )
  )

(defn -run-automata-rules-on-random-individual-cells
  [js-grid w h survival-threshold birth-threshold iterations]
  (loop [i 0
         active-cells []]
    (if (= i iterations)
      active-cells

      (let [x (rand-int w)
            y (rand-int h)
            new-value (-new-value-at-position js-grid x y w h survival-threshold birth-threshold)]
        (-> js-grid
            (aget y)
            (aset x new-value))
        (recur (inc i)
               (conj active-cells [[x y] new-value]))))))

(defn ^:export automata
  [w h initial-wall-probability survival-threshold birth-threshold iterations smoothing-passes]
  (let [js-grid (-make-js-grid w h initial-wall-probability)
        cljs-initial-grid (array->grid js-grid)
        history (-run-automata-rules-on-random-individual-cells
                  js-grid w h survival-threshold birth-threshold iterations)]

    {::grid         (array->grid js-grid)
     ::initial-grid cljs-initial-grid
     ::history      history}))

#_(stest/instrument [`drunkards-walk])

(comment
  (tufte/add-basic-println-handler! {})

  (let [grid (full-grid 30 30)]
    (js/console.profile "drunkard")
    (dotimes [_ 10]
      (drunkards-walk grid 150))
    (js/console.profileEnd))

  (identical? 1 1)

  (profile
    {}
    (let [grid (full-grid 30 30)]
      (dotimes [_ 100]
        (p :drunkard
           (drunkards-walk grid 150)
           nil))))

  (profile
    {}
    (dotimes [_ 200]
      (p :automata
         (automata 100 100 0.45 3 4 20000 nil)
         nil)))
  )

