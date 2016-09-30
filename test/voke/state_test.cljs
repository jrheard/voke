(ns voke.state-test
  (:require [cljs.test :refer [deftest is testing]]
            [cljs.spec :as s]
            [cljs.spec.test :as stest]
            [clojure.test.check.generators]
            [voke.specs]
            [voke.state :as state]
            [voke.test-utils :refer [blank-game-state game-state-with-an-entity]]))

;(stest/instrument)

(comment
  (first (s/exercise :entity/entity))
  )

(def example-entity
  (assoc
    (first (s/exercise :entity/entity))
    :entity/id
    0))

(deftest adding-entities
  (with-redefs [state/buffer (atom [])]
    (state/add-entity! example-entity :combat-system)

    (let [updated-state (state/flush! blank-game-state)]
      (is (= (get-in updated-state [:entities 0])
             example-entity)))))


(deftest updating-entities
  (with-redefs [state/buffer (atom [])]

    ; make game-state-with-an-entity just have a random entity with id 0
    (state/update-entity! 0 :combat-system (fn [entity] (assoc entity :component/render {:render/fill 3})))

    (let [updated-state (state/flush! game-state-with-an-entity)]
      (is (= (get-in updated-state [:entities 0])
             {:id 0 :renderable true})))))

(deftest removing-entities
  (with-redefs [state/buffer (atom [])]
    (state/remove-entity! 0 :combat-system)
    (let [updated-state (state/flush! game-state-with-an-entity)]
      (is (= false
             (contains? (updated-state :entities) 0))))))
