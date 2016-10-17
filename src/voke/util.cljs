(ns voke.util
  (:require [cljs.core.async :refer [chan close!]]))

(defn in?
  "per https://stackoverflow.com/questions/3249334/test-whether-a-list-contains-a-specific-value-in-clojure"
  [coll elm]
  (some #(= elm %) coll))

(defn bound-between
  [num lower upper]
  (cond
    (< num lower) lower
    (> num upper) upper
    :else num))

(defn winnow
  [pred xs]
  (let [matches (transient [])
        not-matches (transient [])]
    (doseq [x xs]
      (if (pred x)
        (conj! matches x)
        (conj! not-matches x)))
    [(persistent! matches) (persistent! not-matches)]))

; taken directly from https://github.com/sjl/roul/blob/master/src/roul/random.clj
(defn rand-nth-weighted
  "Return a random element from the weighted collection.
  A weighted collection can be any seq of [choice, weight] elements.  The
  weights can be arbitrary numbers -- they do not need to add up to anything
  specific.
  Examples:
  (rand-nth-weighted [[:a 0.50] [:b 0.20] [:c 0.30]])
  (rand-nth-weighted {:a 10 :b 200})
  "
  [coll]
  (let [total (reduce + (map second coll))]
    (loop [i (rand total)
           [[choice weight] & remaining] (seq coll)]
      (if (>= weight i)
        choice
        (recur (- i weight) remaining)))))

; from https://gist.github.com/swannodette/5882703
(defn timeout [ms]
  (let [c (chan)]
    (js/setTimeout (fn [] (close! c)) ms)
    c))
