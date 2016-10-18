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

(def ^:export a-grid (full-grid 30 30))

(defn ^:export drunkards-walk [grid num-empty-cells]
  (let [height (count grid)
        width (count (first grid))]

    (loop [grid grid
           historical-active-cells []
           x (rand-int width)
           y (rand-int height)
           empty-cells 0]

      (if (= empty-cells num-empty-cells)
        {::grid    grid
         ::history historical-active-cells}

        (let [cell-was-full? (= (get-in grid [y x]) :full)
              horizontal-direction-to-center (if (< x (/ width 2)) :east :west)
              vertical-direction-to-center (if (< y (/ height 2)) :south :north)
              direction (rand-nth-weighted
                          (into {}
                                (map (fn [direction]
                                       (if (#{horizontal-direction-to-center vertical-direction-to-center}
                                             direction)
                                         [direction 1.4]
                                         [direction 1.0]))
                                     [:north :south :east :west])))]

          (recur (assoc-in grid [y x] :empty)
                 (conj historical-active-cells [x y])
                 (case direction
                   :east (bound-between (inc x) 0 (dec width))
                   :west (bound-between (dec x) 0 (dec width))
                   x)
                 (case direction
                   :north (bound-between (dec y) 0 (dec height))
                   :south (bound-between (inc y) 0 (dec height))
                   y)
                 (if cell-was-full?
                   (inc empty-cells)
                   empty-cells)))))))

(s/fdef drunkards-walk
  :args (s/cat :grid ::grid
               :num-empty-cells nat-int?)
  :ret ::grid-with-history)

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

