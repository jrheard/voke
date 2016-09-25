(ns voke.events-test
  (:require [cljs.test :refer [deftest is testing]]
            [voke.events :as events]))

(deftest event-system
  (let [foo-counter (atom 0)
        bar-counter (atom 0)]
    (events/subscribe-to-event :foo #(swap! foo-counter inc))
    (events/subscribe-to-event :bar #(swap! bar-counter inc))

    (events/publish-event {:type :foo})
    (is (= @foo-counter 1))
    (is (= @bar-counter 0))

    (events/publish-event {:type :bar})
    (is (= @foo-counter 1))
    (is (= @bar-counter 1))

    (events/unsub-all!)
    (events/publish-event {:type :foo})
    (events/publish-event {:type :bar})
    (is (= @foo-counter 1))
    (is (= @bar-counter 1))))
