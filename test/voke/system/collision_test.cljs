(ns voke.system.collision-test
  (:require [cljs.test :refer [deftest is testing]]
            [js-collision]
            [rbush]
            [voke.events :as events]
            [voke.system.collision.system :as system]
            [voke.system.collision.state :as state]
            [voke.system.collision.util :as collision-util]
            [voke.test-utils :refer [blank-game-state]]))

(deftest two-entities-moving-into-each-other
  (let [entity-1 {:id        1
                  :collision {:type :foo}
                  :shape     {:width  10
                              :height 10
                              :center {:x 100
                                       :y 100}}}
        entity-2 {:id        2
                  :collision {:type :foo}
                  :shape     {:width  10
                              :height 10
                              :center {:x 115
                                       :y 115}}}
        game-state (-> blank-game-state
                       (assoc-in [:entities 1] entity-1)
                       (assoc-in [:entities 2] entity-2))
        collision-events (atom [])
        apply-movement-calls (atom [])]

    (collision-util/-track-entity entity-1)
    (collision-util/-track-entity entity-2)

    (events/subscribe-to-event :contact
                               (fn [event]
                                 (swap! collision-events conj event)))


    (with-redefs [state/contacts-fired (atom #{})
                  collision-util/apply-movement (fn [& args]
                                                  (swap! apply-movement-calls conj args))]
      (system/attempt-to-move! entity-1
                               {:x 105 :y 105}
                               {:x 5 :y 5}
                               (vals (game-state :entities)))
      (system/attempt-to-move! entity-2
                               {:x 105 :y 105}
                               {:x 5 :y 5}
                               (vals (game-state :entities))))

    (testing "when two entities try to move into the same space, only one collision event should be fired"
      (is (= @collision-events
             [{:type     :contact
               :entities [entity-1 entity-2]}])))

    (testing "diagonal collision resolution works correctly on corners"
      (is (= @apply-movement-calls
             [[entity-1 {:x 104.9 :y 104.9} {:x 0 :y 0}]
              [entity-2 {:x 99.9 :y 99.9} {:x 0 :y 0}]])))


    (js/Collision.resetState)))

; next up: entity is moving up+right, is next to a wall on its right


; also test dead entities / projectiles

; test that collision type / collides-with works