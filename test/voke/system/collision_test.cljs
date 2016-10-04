(ns voke.system.collision-test
  (:require [cljs.test :refer [deftest is testing]]
            [js-collision]
            [rbush]
            [voke.events :as events]
            [voke.system.collision-test-macros :refer-macros [with-collision-env]]
            [voke.system.collision.system :as system]
            [voke.system.collision.state :as state]
            [voke.system.collision.util :as collision-util]
            [voke.test-utils :refer [blank-game-state]]))

(defn make-entity
  ([id x y] (make-entity id x y {:collision/type :gold}))
  ([id x y collision-component]
   #:component{:entity/id id
               :collision collision-component
               :shape     #:shape{:width  10
                                  :height 10
                                  :center #:geometry{:x x
                                                     :y y}}}))

(defn prepare-for-collision-test [entities collision-events-atom]
  ; can't *completely* set up the state of the world, because with-redefs is a macro
  ; if we wanted to really DRY up these tests, we'd have to write a clj-land macro.
  ; putting that off for now.
  (doseq [entity entities]
    (collision-util/-track-entity entity))

  (events/subscribe-to-event :contact
                             (fn [event]
                               (swap! collision-events-atom conj event)))

  (assoc blank-game-state
         :game-state/entities
         (into {}
               (map (juxt :entity/id identity) entities))))

(deftest two-entities-moving-into-each-other-diagonally
  (let [entity-1 (make-entity 1 100 100)
        entity-2 (make-entity 2 115 115)]
    (with-collision-env [entity-1 entity-2] collision-events apply-movement-calls game-state
      ; Two 10x10 entities, moving diagonally into the same spot.
      (system/attempt-to-move! entity-1
                               #:geometry{:x 105 :y 105}
                               #:geometry{:x 5 :y 5}
                               (vals (game-state :game-state/entities)))
      (system/attempt-to-move! entity-2
                               #:geometry{:x 105 :y 105}
                               #:geometry{:x 5 :y 5}
                               (vals (game-state :game-state/entities)))

      (testing "when two entities try to move into the same space, only one collision event should be fired"
        (is (= @collision-events
               [{:event/type :contact
                 :entities   [entity-1 entity-2]}])))

      (testing "diagonal collision resolution works correctly on corners"
        (is (= @apply-movement-calls
               [[entity-1 #:geometry{:x 104.9 :y 104.9} #:geometry{:x 0 :y 0}]
                [entity-2 #:geometry{:x 99.9 :y 99.9} #:geometry{:x 0 :y 0}]]))))))


#_(deftest two-entities-moving-into-each-other-diagonally
    (let [entity-1 (make-entity 1 100 100)
          entity-2 (make-entity 2 115 115)
          collision-events (atom [])
          apply-movement-calls (atom [])
          game-state (prepare-for-collision-test [entity-1 entity-2] collision-events)]

      )

    (js/Collision.resetState))

(deftest one-entity-moving-against-another
  (let [entity-1 (make-entity 1 100 100)
        entity-2 (make-entity 2 111 100)
        collision-events (atom [])
        apply-movement-calls (atom [])
        game-state (prepare-for-collision-test [entity-1 entity-2] collision-events)]

    (with-redefs [state/contacts-fired (atom #{})
                  collision-util/apply-movement (fn [& args]
                                                  (swap! apply-movement-calls conj args))]
      (system/attempt-to-move! entity-1
                               #:geometry{:x 105 :y 103}
                               #:geometry{:x 5 :y 3}
                               (vals (game-state :game-state/entities))))

    (is (= @collision-events
           [{:event/type :contact
             :entities   [entity-1 entity-2]}]))

    (testing "two entities next to each other, the left one moving up+right; it should be able to
      move all the way up and a little bit to the right"
      (is (= @apply-movement-calls
             [[entity-1 #:geometry{:x 100.99 :y 103} #:geometry{:x 0 :y 3}]]))))

  (js/Collision.resetState))

(deftest projectiles-and-collides-with
  (let [entity-1 (make-entity 1 100 100 #:collision{:type :projectile :collides-with #{:obstacle} :destroyed-on-contact true})
        entity-2 (make-entity 2 111 100 #:collision{:type :projectile :collides-with #{:obstacle} :destroyed-on-contact true})
        entity-3 (make-entity 3 122 100 #:collision{:type :obstacle})
        collision-events (atom [])
        apply-movement-calls (atom [])
        remove-entity-calls (atom [])
        game-state (prepare-for-collision-test [entity-1 entity-2 entity-3] collision-events)]

    (with-redefs [state/contacts-fired (atom #{})
                  collision-util/apply-movement (fn [& args]
                                                  (swap! apply-movement-calls conj args))
                  collision-util/remove-entity! (fn [& args]
                                                  (swap! remove-entity-calls conj args))]
      (system/attempt-to-move! entity-1
                               #:geometry{:x 105 :y 100}
                               #:geometry{:x 5 :y 0}
                               (vals (game-state :game-state/entities)))

      ; entities 1 and 2 are projectiles and shouldn't collide with one another
      (is (= @collision-events []))
      (is (= @remove-entity-calls []))
      (is (= @apply-movement-calls
             [[entity-1 #:geometry{:x 105 :y 100} #:geometry{:x 5 :y 0}]]))

      (reset! apply-movement-calls [])

      ; entity 3 is a wall, though, and so entity 1 should collide with it and be destroyed
      (system/attempt-to-move! entity-1
                               #:geometry{:x 120 :y 100}
                               #:geometry{:x 15 :y 0}
                               (vals (game-state :game-state/entities)))

      (is (= @collision-events
             [{:event/type :contact
               :entities   [entity-1 entity-3]}]))
      (is (= @apply-movement-calls []))
      (is (= @remove-entity-calls
             [[entity-1]]))))

  (js/Collision.resetState))

(deftest an-entity-moving-into-many-entities
  (let [entity-1 (make-entity 1 100 100)
        entity-2 (make-entity 2 111 101)
        entity-3 (make-entity 3 112 102)
        entity-4 (make-entity 4 111 102)
        entity-5 (make-entity 5 111 100)
        collision-events (atom [])
        apply-movement-calls (atom [])
        game-state (prepare-for-collision-test [entity-1 entity-2 entity-3 entity-4 entity-5] collision-events)]

    (with-redefs [state/contacts-fired (atom #{})
                  collision-util/apply-movement (fn [& args]
                                                  (swap! apply-movement-calls conj args))]
      (system/attempt-to-move! entity-1
                               #:geometry{:x 105 :y 103}
                               #:geometry{:x 5 :y 3}
                               (vals (game-state :game-state/entities))))

    (is (= @collision-events
           [{:event/type :contact
             :entities   [entity-1 entity-2]}
            {:event/type :contact
             :entities   [entity-1 entity-3]}
            {:event/type :contact
             :entities   [entity-1 entity-4]}
            {:event/type :contact
             :entities   [entity-1 entity-5]}]))

    (testing "entity-1 should be moved so it's right up against entity-5"
      (is (= @apply-movement-calls
             [[entity-1 #:geometry{:x 100.99 :y 103} #:geometry{:x 0 :y 3}]]))))

  (js/Collision.resetState))
