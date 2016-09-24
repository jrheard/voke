(ns voke.state-test
  (:require [cljs.test :refer [deftest is testing]]
            [voke.state :as state]))

(def blank-game-state (state/make-game-state []))
(def game-state-with-an-entity (assoc-in blank-game-state
                                         [:entities 0]
                                         {:id 0 :renderable false}))

(deftest adding-entities
  (with-redefs [state/buffer (atom [])]
    (state/add-entity! {:id 0 :renderable false} :combat-system)

    (let [updated-state (state/flush! blank-game-state)]
      (is (= (get-in updated-state [:entities 0])
             {:id 0 :renderable false})))))


(deftest updating-entities
  (with-redefs [state/buffer (atom [])]

    (state/update-entity! 0 :combat-system (fn [entity] (assoc entity :renderable true)))

    (let [updated-state (state/flush! game-state-with-an-entity)]
      (is (= (get-in updated-state [:entities 0])
             {:id 0 :renderable true})))))

(deftest removing-entities
  (with-redefs [state/buffer (atom [])]
    (state/remove-entity! 0 :combat-system)
    (let [updated-state (state/flush! game-state-with-an-entity)]
      (is (= false
             (contains? (updated-state :entities) 0))))))
