(ns voke.clock-test
  (:require [cljs.test :refer [deftest is]]
            [cljs.spec.test :as stest]
            [voke.clock :as clock]))

; TODO actually add an assert
(deftest generative
  (stest/check `clock/add-time!))
