(ns voke.input-test
  (:require [cljs.core.async :refer [chan <! put!]]
            [cljs.test :refer [async deftest is testing]]
            [goog.events :as events]
            [voke.input :as input])
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
                                             :preventDefault (fn [])})) ]

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


(deftest handle-keyboard-events
  )