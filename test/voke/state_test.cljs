(ns voke.state-test
  (:require [cljs.test :refer [deftest is testing]]
            [voke.state :as state]))

(def blank-game-state (state/make-game-state []))

(deftest adding-entities
  (with-redefs [state/buffer (atom [])]
    (state/add-entity! {:id 0 :renderable false} :combat-system)

    (let [updated-state (state/flush! blank-game-state)]
      (is (= (get-in updated-state [:entities 0])
             {:id 0 :renderable false})))))


