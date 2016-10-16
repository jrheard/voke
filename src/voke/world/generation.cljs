(ns voke.world.generation
  (:require [cljs.spec :as s]
            [cljs.spec.test :as stest]
            [voke.util :refer [bound-between rand-nth-weighted]]))

(s/def ::cell #{:empty :full})
(s/def ::grid (s/coll-of (s/coll-of ::cell)))
(s/def ::historical-active-cells (s/coll-of ::cell))
(s/def ::world (s/keys :req [::grid ::historical-active-cells]))

(defn full-grid [w h]
  (vec (repeat h
               (vec (repeat w :full)))))

(s/fdef full-grid
  :args (s/cat :w nat-int? :h nat-int?)
  :ret ::grid)

(defn grid->str [grid]
  (apply str
         (map (fn [line]
                (str (apply str
                            (map (fn [cell]
                                   (if (= cell :full)
                                     "X"
                                     " "))
                                 line))
                     "\n"))
              grid)))

(defn count-empty-spaces [grid]
  (apply +
         (map (fn [line]
                (count
                  (filter #(= % :empty) line)))
              grid)))

(s/fdef count-empty-spaces
  :args (s/cat :grid ::grid)
  :ret nat-int?)

(defn drunkards-walk [grid num-empty-cells]
  (let [height (count grid)
        width (count (first grid))]

    (loop [grid grid
           historical-active-cells []
           x (rand-int width)
           y (rand-int height)]

      (if (= (count-empty-spaces grid)
             num-empty-cells)
        {::grid         grid
         ::historical-active-cells historical-active-cells}

        (let [horizontal-direction-to-center (if (< x (/ width 2)) :east :west)
              vertical-direction-to-center (if (< y (/ height 2)) :south :north)
              direction (rand-nth-weighted
                          (into {}
                                (map (fn [direction]
                                       (if (#{horizontal-direction-to-center vertical-direction-to-center}
                                             direction)
                                         [direction 1.2]
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
                   y)))))))

(s/fdef drunkards-walk
  :args (s/cat :grid ::grid
               :num-empty-cells nat-int?)
  :ret ::world)

(stest/instrument [`drunkards-walk
                   `full-grid
                   `count-empty-spaces])

(comment
  (-> ::grid
      (s/exercise 1)
      ffirst
      grid->str
      print
      )

  (-> (full-grid 50 20)
      (drunkards-walk 150)
      ;::active-cells
      ;count

      ::grid
      grid->str
      print
      )

  )
