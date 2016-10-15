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
           [x y] [(rand-int width) (rand-int height)]]

      (if (= (count-empty-spaces grid)
             num-empty-cells)
        grid

        (recur (assoc-in grid [y x] :empty)
               (case (rand-nth [:north :east :south :west])
                 :north [x
                         (bound-between (dec y) 0 height)]
                 :east [(bound-between (inc x) 0 width)
                        y]
                 :south [x
                         (bound-between (inc y) 0 height)]
                 :west [(bound-between (dec x) 0 width)
                        y]))))))

(comment
  (-> ::grid
      (s/exercise 1)
      ffirst
      grid->str
      print
      )

  (-> (full-grid 50 20)
      (drunkards-walk 250)
      grid->str
      print
      )

  )
