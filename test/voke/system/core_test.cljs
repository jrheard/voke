(ns voke.system.core-test
  (:require [cljs.test :refer [deftest is testing]]
            [voke.system.core :as core]
            [voke.test-utils :refer [blank-game-state game-state-with-an-entity]]))

(deftest system-to-tick-function
  (let [system {:system/tick-fn (fn [entities]
                                  [(assoc (first entities) :foo :bar)])}
        tick-fn (core/system-to-tick-fn system)]

    (is (= (tick-fn game-state-with-an-entity)
           (assoc-in game-state-with-an-entity [:game-state/entities 0 :foo] :bar)))))

(deftest system-tick-functions-cant-return-new-entities
  (let [system {:system/tick-fn (fn [entities]
                                  (conj entities {:entity/id 123}))}
        tick-fn (core/system-to-tick-fn system)]

    (is (thrown? js/Error (tick-fn blank-game-state)))))