(ns voke.system.collision-test
  (:require [cljs.test :refer [deftest is testing]]
            [js-collision]
            [rbush]
            [voke.events :as events]
            [voke.system.collision.system :as system]
            [voke.system.collision.state :as state]
            [voke.system.collision.util :as collision-util]
            [voke.test-utils :refer [blank-game-state]]))

(defn make-entity [id x y]
  {:id        id
   :collision {:type :foo}
   :shape     {:width  10
               :height 10
               :center {:x x :y y}}})

(defn prepare-for-collision-test [entities collision-events-atom]
  (doseq [entity entities]
    (collision-util/-track-entity entity))

  (events/subscribe-to-event :contact
                             (fn [event]
                               (swap! collision-events-atom conj event)))

  (assoc blank-game-state
         :entities
         (into {}
               (map (juxt :id identity) entities))))

(deftest two-entities-moving-into-each-other
  (let [entity-1 (make-entity 1 100 100)
        entity-2 (make-entity 2 115 115)
        collision-events (atom [])
        apply-movement-calls (atom [])
        game-state (prepare-for-collision-test [entity-1 entity-2] collision-events)]

    (with-redefs [state/contacts-fired (atom #{})
                  collision-util/apply-movement (fn [& args]
                                                  (swap! apply-movement-calls conj args))]
      ; Two 10x10 entities, moving diagonally into the same spot.
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