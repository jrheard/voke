(ns voke.system.core-test
  (:require [cljs.test :refer [deftest is testing]]
            [voke.system.core :as core]
            [voke.test-utils :refer [blank-game-state game-state-with-an-entity]]))

(deftest system-to-tick-function
  (let [system {:tick-fn (fn [entities]
                           [(assoc (first entities) :foo :bar)])}
        tick-fn (core/system-to-tick-fn system)]
    (is (= (tick-fn game-state-with-an-entity)
           (assoc-in game-state-with-an-entity [:entities 0 :foo] :bar)))))
