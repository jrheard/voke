(ns voke.util)

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
