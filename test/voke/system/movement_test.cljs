(ns voke.system.movement-test
  (:require [cljs.test :refer [deftest is testing]]
            [voke.system.movement :as movement]
            [voke.test-utils :refer [blank-game-state game-state-with-an-entity truthy? falsy?]]))

(deftest relevant-to-movement-system
  (is (truthy? (movement/relevant-to-movement-system? {:motion {:direction 1}})))
  (is (truthy? (movement/relevant-to-movement-system? {:motion {:velocity {:x 0.5 :y 0}}})))

  (is (falsy? (movement/relevant-to-movement-system? {:motion {:direction nil}})))
  (is (falsy? (movement/relevant-to-movement-system? {:motion {:velocity {:x 0 :y 0}}})))
  (is (falsy? (movement/relevant-to-movement-system? {:foo :bar}))))

(deftest should-update-velocity
  (is (truthy? (movement/should-update-velocity? {:motion {:direction 1}})))
  (is (truthy? (movement/should-update-velocity? {:motion {:affected-by-friction true
                                                           :velocity {:x 0.5 :y 0}}})))

  (is (falsy? (movement/should-update-velocity? {:motion {:direction nil}})))
  (is (falsy? (movement/should-update-velocity? {:motion {:affected-by-friction true
                                                           :velocity {:x 0 :y 0}}})))
  (is (falsy? (movement/should-update-velocity? {:foo :bar}))))
