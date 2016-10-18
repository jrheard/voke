(ns voke.world.generation
  (:require [cljs.spec :as s]
            [cljs.spec.test :as stest]
            [taoensso.tufte :as tufte :refer-macros [p profiled profile]]
            [voke.util :refer [bound-between rand-nth-weighted]]))

(s/def ::cell #{:empty :full})
(s/def ::width nat-int?)
(s/def ::height nat-int?)
(s/def ::grid (s/coll-of (s/coll-of ::cell)))

(s/def ::history (s/coll-of ::cell))
(s/def ::generated-level (s/keys :req [::grid ::history]))

(defn full-grid [w h]
  (vec (repeat h
               (vec (repeat w :full)))))

(s/fdef full-grid
  :args (s/cat :w nat-int? :h nat-int?)
  :ret ::grid)

(defn count-empty-spaces [grid]
  (apply +
         (map (fn [line]
                (count
                  (filter #(= % :empty) line)))
              grid)))

(s/fdef count-empty-spaces
  :args (s/cat :grid ::grid)
  :ret nat-int?)

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
                                       [direction 1.4]
                                       [direction 1.0]))
                                   [:north :south :east :west])))]

        (recur (assoc-in grid [y x] :empty)
               (conj historical-active-cells [[x y] :full])
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

(defn automata
  [w h initial-wall-probability iterations]
  (let [generate-row (fn [] (vec
                              (take w
                                    (repeatedly #(rand-nth-weighted {:empty (- 1 initial-wall-probability)
                                                                     :full  initial-wall-probability})))))
        initial-grid (vec (take h (repeatedly generate-row)))
        new-value-at-position (fn [grid x y]
                                (let [cell-is-full? (= (get-in grid [y x]) :full)
                                      neighbors (for [i (range (dec x)
                                                               (+ x 2))
                                                      j (range (dec y)
                                                               (+ y 2))
                                                      :when (not= [i j] [x y])]
                                                  (if (or (< i 0)
                                                          (>= i w)
                                                          (< j 0)
                                                          (>= j h))
                                                    :full
                                                    (get-in grid [j i])))
                                      num-full-neighbors (count (filter #(= % :full) neighbors))]
                                  (cond
                                    (and cell-is-full?
                                         (> num-full-neighbors 2)) :full
                                    (and (not cell-is-full?)
                                         (> num-full-neighbors 5)) :full
                                    :else :empty)))]

    (loop [i 0
           grid initial-grid
           active-cells []]
      (js/console.log i (count active-cells))
      (if (= i iterations)
        {::grid         grid
         ::initial-grid initial-grid
         ::history      active-cells}

        (let [x (rand-int w)
              y (rand-int h)
              new-value (new-value-at-position grid x y)]
          (recur (inc i)
                 (assoc-in grid [y x] new-value)
                 (conj active-cells [[x y] new-value])))))))


#_(stest/instrument [`drunkards-walk
                     `full-grid
                     `count-empty-spaces])

(comment
  (tufte/add-basic-println-handler! {})

  (let [grid (full-grid 30 30)]
    (js/console.profile "drunkard")
    (dotimes [_ 10]
      (drunkards-walk grid 150))
    (js/console.profileEnd))

  ; master benchmarking command
  (profile
    {}
    (let [grid (full-grid 30 30)]
      (dotimes [_ 100]
        (p :drunkard
           (drunkards-walk grid 150)
           nil))))
  (profile
    {}
    (p :full-grid
       (dotimes [_ 100]
         (full-grid 50 50))))
  )

