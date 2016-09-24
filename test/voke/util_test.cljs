(ns voke.util-test
  (:require [cljs.test :refer [deftest is testing]]
            [voke.util :as util]))

(deftest testing-in
  (is (true? (util/in? (range 5) 3)))
  (is (not (util/in? (range 5) 10))))

(deftest bound-between
  (is (= (util/bound-between 10 100 500)
         100))

  (is (= (util/bound-between 250 100 500)
         250))

  (is (= (util/bound-between 50000 100 500)
         500)))

(deftest testing-winnow
  (is (= [[0 2 4] [1 3]]
         (util/winnow even? (range 5)))))
