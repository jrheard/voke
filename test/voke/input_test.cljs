(ns voke.input-test
  (:require [cljs.core.async :refer [chan <! put! >!]]
            [cljs.test :refer [async deftest is testing]]
            [goog.events :as events]
            [voke.input :as input]
            [voke.state :as state]
            [voke.test-utils :refer [blank-game-state]])
  (:import [goog.events KeyCodes])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(deftest listen-to-keyboard-inputs
  (async done
    (go
      (let [event-chan (chan)
            event-handlers (atom [])]
        (with-redefs [events/listen (fn [_ _ event-handler-fn]
                                      (swap! event-handlers conj event-handler-fn))]
          (input/listen-to-keyboard-inputs event-chan)

          (let [[down-handler up-handler] @event-handlers
                send-event-to-handler (fn [handler key-code]
                                        (handler #js {:keyCode        key-code
                                                      :preventDefault (fn [])}))]

            (testing "basic keydown functionality"
              (send-event-to-handler down-handler KeyCodes.W)
              (send-event-to-handler down-handler KeyCodes.A)
              (is (= (<! event-chan)
                     {:type      :move-key-down
                      :direction :up}))
              (is (= (<! event-chan)
                     {:type      :move-key-down
                      :direction :left})))

            (testing "basic keyup functionality"
              (send-event-to-handler up-handler KeyCodes.W)
              (send-event-to-handler up-handler KeyCodes.A)
              (is (= (<! event-chan)
                     {:type      :move-key-up
                      :direction :up}))
              (is (= (<! event-chan)
                     {:type      :move-key-up
                      :direction :left}))))))
      (done))))

(deftest handle-keyboard-events-movement
  (async done
    (go
      (let [stub-event-chan (atom nil)
            game-state (-> blank-game-state
                           (assoc-in [:entities 0 :input]
                                     {:intended-move-direction #{}}))]
        (with-redefs [input/listen-to-keyboard-inputs (fn [event-chan]
                                                        (reset! stub-event-chan event-chan))]
          (input/handle-keyboard-events 0)

          ; press down+right
          (>! @stub-event-chan {:type      :move-key-down
                                :direction :down})
          (>! @stub-event-chan {:type      :move-key-down
                                :direction :right})
          (>! @stub-event-chan {:type :noop})

          (let [game-state (state/flush! game-state)]
            (is (= (get-in game-state [:entities 0 :input :intended-move-direction])
                   #{:down :right}))

            (is (= (get-in game-state [:entities 0 :motion :direction])
                   (input/intended-directions->angle #{:down :right})))

            ; let go of down
            (>! @stub-event-chan {:type      :move-key-up
                                  :direction :down})
            (>! @stub-event-chan {:type :noop})

            (let [game-state (state/flush! game-state)]
              (is (= (get-in game-state [:entities 0 :input :intended-move-direction])
                     #{:right}))

              (is (= (get-in game-state [:entities 0 :motion :direction])
                     (input/intended-directions->angle #{:right})))

              ; let go of right
              (>! @stub-event-chan {:type      :move-key-up
                                    :direction :right})
              (>! @stub-event-chan {:type :noop})

              (let [game-state (state/flush! game-state)]
                (is (= (get-in game-state [:entities 0 :input :intended-move-direction])
                       #{}))

                (is (= (get-in game-state [:entities 0 :motion :direction])
                       nil)))))))
      (done))))

(deftest handle-keyboard-events-firing
  (async done
    (go
      (let [stub-event-chan (atom nil)
            game-state (-> blank-game-state
                           (assoc-in [:entities 0 :input]
                                     {:intended-fire-direction []}))]
        (with-redefs [input/listen-to-keyboard-inputs (fn [event-chan]
                                                        (reset! stub-event-chan event-chan))]
          (input/handle-keyboard-events 0)

          ; press down+right
          (>! @stub-event-chan {:type      :fire-key-down
                                :direction :down})
          (>! @stub-event-chan {:type      :fire-key-down
                                :direction :right})
          (>! @stub-event-chan {:type :noop})

          (let [game-state (state/flush! game-state)]
            (is (= (get-in game-state [:entities 0 :input :intended-fire-direction])
                   [:down :right]))

            (is (= (get-in game-state [:entities 0 :weapon :fire-direction])
                   (input/intended-directions->angle [:right])))

            ; let go of right
            (>! @stub-event-chan {:type      :fire-key-up
                                  :direction :right})
            (>! @stub-event-chan {:type :noop})

            (let [game-state (state/flush! game-state)]
              (is (= (get-in game-state [:entities 0 :input :intended-fire-direction])
                     [:down]))

              (is (= (get-in game-state [:entities 0 :weapon :fire-direction])
                     (input/intended-directions->angle [:down])))

              ; let go of down
              (>! @stub-event-chan {:type      :fire-key-up
                                    :direction :down})
              (>! @stub-event-chan {:type :noop})

              (let [game-state (state/flush! game-state)]
                (is (= (get-in game-state [:entities 0 :input :intended-fire-direction])
                       []))

                (is (= (get-in game-state [:entities 0 :weapon :fire-direction])
                       nil)))))))
      (done))))

(deftest remove-conflicting-directions
  (is (= (input/remove-conflicting-directions #{:down :left :right})
         #{:down}))
  (is (= (input/remove-conflicting-directions #{:down :left :right :up})
         #{}))
  (is (= (input/remove-conflicting-directions #{:down})
         #{:down}))
  (is (= (input/remove-conflicting-directions #{:down :up})
         #{}))
  (is (= (input/remove-conflicting-directions #{})
         #{})))

(deftest intended-directions->angle
  (is (= (input/intended-directions->angle [:left])
         Math/PI))
  (is (= (input/intended-directions->angle [:right])
         0))
  (is (= (input/intended-directions->angle [:up :right])
         (- (/ Math/PI 4))))
  (is (= (input/intended-directions->angle [:down :left])
         (/ (* 3 Math/PI) 4))))
