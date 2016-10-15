(ns voke.world.generation
  (:require [cljs.spec :as s]
            [voke.util :refer [bound-between]]))

(s/def ::cell #{:empty :full})
(s/def ::grid (s/coll-of (s/coll-of ::cell :count 20)
                         :count 20))

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
           x (rand-int width)
           y (rand-int height)]

      (if (= (count-empty-spaces grid)
             num-empty-cells)
        grid

        (let [base-directions [:north :south :east :west]
              horizontal-direction-to-center (if (< x (/ width 2)) :east :west)
              vertical-direction-to-center (if (< y (/ height 2)) :south :north)
              direction (rand-nth (conj base-directions
                                        horizontal-direction-to-center
                                        vertical-direction-to-center))]

          (recur (assoc-in grid [y x] :empty)
                 (case direction
                   :east (bound-between (inc x) 0 width)
                   :west (bound-between (dec x) 0 width)
                   x)
                 (case direction
                   :north (bound-between (dec y) 0 height)
                   :south (bound-between (inc y) 0 height)
                   y)))))))

(comment
  (-> ::grid
      (s/exercise 1)
      ffirst
      grid->str
      print
      )

  (-> (full-grid 50 20)
      (drunkards-walk 200)
      grid->str
      print
      )

  )
