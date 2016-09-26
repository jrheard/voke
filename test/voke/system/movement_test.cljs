(ns voke.system.movement-test
  (:require [cljs.test :refer [deftest is testing]]
            [voke.system.core :refer [system-to-tick-fn]]
            [voke.system.collision.system :as collision]
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

(deftest move-system-tick
  (testing "the movement system should call attempt-to-move! once per each relevant moving entity"
    (let [tick-fn (system-to-tick-fn movement/move-system)]
      (let [initial-state (-> blank-game-state
                              (assoc-in [:entities 0]
                                        {:id     0
                                         :shape  {:center {:x 10 :y 10}}
                                         :motion {:direction        0
                                                  :velocity         {:x 0 :y 0}
                                                  :max-acceleration 1
                                                  :max-speed        10}})
                              (assoc-in [:entities 1]
                                        {:id 1 :motion {:direction nil}})
                              (assoc-in [:entities 2]
                                        {:id 2 :foo :bar}))
            attempt-to-move-args (atom [])]

        (with-redefs [collision/attempt-to-move! (fn [& args]
                                                   (swap! attempt-to-move-args conj args))]
          (tick-fn initial-state)

          (is (= (count @attempt-to-move-args) 1))

          (let [[_ new-center new-velocity _] (first @attempt-to-move-args)]
            (is (= new-center {:x 11 :y 10}))
            (is (= new-velocity {:x 1 :y 0}))))))))
