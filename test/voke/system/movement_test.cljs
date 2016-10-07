(ns voke.system.movement-test
  (:require [cljs.test :refer [deftest is testing]]
            [voke.system.core :refer [system-to-tick-fn]]
            [voke.system.collision.system :as collision]
            [voke.system.movement :as movement]
            [voke.test-utils :refer [blank-game-state game-state-with-an-entity]]))

(deftest relevant-to-movement-system
  (is (movement/relevant-to-movement-system? {:component/motion {:motion/direction 1}}))
  (is (movement/relevant-to-movement-system? {:component/motion {:motion/velocity #:geometry{:x 0.5 :y 0}}}))

  (is (not (movement/relevant-to-movement-system? {:component/motion {:motion/direction nil}})))
  (is (not (movement/relevant-to-movement-system? {:component/motion {:motion/velocity #:geometry{:x 0 :y 0}}})))
  (is (not (movement/relevant-to-movement-system? {:foo :bar}))))

(deftest should-update-velocity
  (is (movement/should-update-velocity? {:component/motion {:motion/direction 1}}))
  (is (movement/should-update-velocity? {:component/motion {:motion/affected-by-friction true
                                                            :motion/velocity             #:geometry{:x 0.5 :y 0}}}))

  (is (not (movement/should-update-velocity? {:component/motion {:motion/direction nil}})))
  (is (not (movement/should-update-velocity? {:component/motion {:affected-by-friction true
                                                                 :motion/velocity      #:geometry{:x 0 :y 0}}})))
  (is (not (movement/should-update-velocity? {:foo :bar}))))

(deftest move-system-tick
  (testing "the movement system should call attempt-to-move! once per each relevant moving entity"
    (let [pi-over-4 (/ Math/PI 4)
          tick-fn (system-to-tick-fn movement/system)
          initial-state (-> blank-game-state
                            (assoc-in [:game-state/entities 0]
                                      {:entity/id        0
                                       :component/shape  #:shape{:center #:geometry{:x 10 :y 10}}
                                       :component/motion #:motion{:direction        pi-over-4
                                                                  :velocity         #:geometry{:x 0 :y 0}
                                                                  :max-acceleration 1
                                                                  :max-speed        1}})
                            (assoc-in [:game-state/entities 1]
                                      {:entity/id 1 :component/motion {:motion/direction nil}})
                            (assoc-in [:game-state/entities 2]
                                      {:entity/id 2 :foo :bar}))
          attempt-to-move-args (atom [])]

      (with-redefs [collision/attempt-to-move! (fn [& args]
                                                 (swap! attempt-to-move-args conj args))]
        (tick-fn initial-state)

        (is (= (count @attempt-to-move-args) 1))

        (let [[_ new-center new-velocity _] (first @attempt-to-move-args)]
          (is (= (.toFixed 4 (new-center :geometry/x))
                 (.toFixed 4 (+ 10 (/ 1 (Math/sqrt 2))))))
          (is (= (.toFixed 4 (new-center :geometry/y))
                 (.toFixed 4 (+ 10 (/ 1 (Math/sqrt 2))))))
          (is (= (.toFixed 4 (new-velocity :geometry/x))
                 (.toFixed 4 (/ 1 (Math/sqrt 2)))))
          (is (= (.toFixed 4 (new-velocity :geometry/y))
                 (.toFixed 4 (/ 1 (Math/sqrt 2))))))))))
