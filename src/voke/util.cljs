(ns voke.util)

(defn in?
  "per https://stackoverflow.com/questions/3249334/test-whether-a-list-contains-a-specific-value-in-clojure"
  [coll elm]
  (some #(= elm %) coll))

(defn now
  []
  (.getTime (js/Date.)))
