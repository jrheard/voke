(ns voke.system.collision-test
  (:require [cljs.test :refer [deftest is testing]]
            [js-collision]
            [rbush]
            [voke.system.collision.system :as system]
            [voke.system.collision.state :as state]
            [voke.test-utils :refer [blank-game-state]]))

; have two entities try to move into each other
; verify that only one contact event is fired, and they're both put into a sane place

;; XXXXXX JS COLLISION SYSTEM NEEDS TO KNOW ABOUT THESE GUYS
;; also these tests need to have access to the js collision system!

(deftest two-entities-moving-into-each-other
  (let [entity-1 {:id 1
                  :shape {:width 10
                          :height 10
                          :center {:x 100
                                   :y 100}}}
        entity-2 {:id 2
                  :shape {:width 10
                          :height 10
                          :center {:x 115
                                   :y 115}}}
        game-state (-> blank-game-state
                       (assoc-in [:entities 1] entity-1)
                       (assoc-in [:entities 2] entity-2))]

    (print js/Collision)
    ; next up: register entities with the js collision system

    (with-redefs [state/contacts-fired (atom #{})]
      #_(system/attempt-to-move! entity-1
                               {:x 105 :y 105}
                               {:x 5 :y 5}
                               (vals (game-state :entities)))
      #_(system/attempt-to-move! entity-2
                               {:x 105 :y 105}
                               {:x 5 :y 5}
                               (vals (game-state :entities)))



      )

    )
  )