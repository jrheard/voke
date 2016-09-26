(ns voke.system.movement-test
  (:require [cljs.test :refer [deftest is testing]]
            [voke.system.movement :as movement]
            [voke.test-utils :refer [blank-game-state game-state-with-an-entity]]))

(deftest relevant-to-movement-system
  (is (movement/relevant-to-movement-system? {:motion {:direction 1}}))
  (is (movement/relevant-to-movement-system? {:motion {:velocity {:x 0.5 :y 0}}}))

  (is (not (movement/relevant-to-movement-system? {:motion {:direction nil}})))
  (is (not (movement/relevant-to-movement-system? {:motion {:velocity {:x 0 :y 0}}})))
  (is (not (movement/relevant-to-movement-system? {:foo :bar}))))

(deftest should-update-velocity
  (is (movement/should-update-velocity? {:motion {:direction 1}}))
  (is (movement/should-update-velocity? {:motion {:affected-by-friction true
                                                          :velocity             {:x 0.5 :y 0}}}))

  (is (not (movement/should-update-velocity? {:motion {:direction nil}})))
  (is (not (movement/should-update-velocity? {:motion {:affected-by-friction true
                                                   :velocity             {:x 0 :y 0}}})))
  (is (not (movement/should-update-velocity? {:foo :bar}))))
