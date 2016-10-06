(ns voke.events-test
  (:require [cljs.pprint :refer [pprint]]
            [cljs.spec.test]
            [cljs.test :refer [deftest is testing]]
            [voke.events :as events]
            [voke.test-utils-macros :refer-macros [check]]))

(deftest generative
  (check `events/publish-event))

(deftest event-system
  (let [foo-counter (atom 0)
        bar-counter (atom 0)]
    (events/subscribe-to-event :foo #(swap! foo-counter inc))
    (events/subscribe-to-event :bar #(swap! bar-counter inc))

    (events/publish-event {:event/type :foo})
    (is (= @foo-counter 1))
    (is (= @bar-counter 0))

    (events/publish-event {:event/type :bar})
    (is (= @foo-counter 1))
    (is (= @bar-counter 1))

    (events/unsub-all!)
    (events/publish-event {:event/type :foo})
    (events/publish-event {:event/type :bar})
    (is (= @foo-counter 1))
    (is (= @bar-counter 1))))
