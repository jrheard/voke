(ns voke.clock-test
  (:require [cljs.test :refer [deftest is]]
            [cljs.spec.test]
            [voke.clock :as clock]
            [voke.test-utils-macros :refer-macros [check]]))

(deftest generative
  (check `clock/add-time!))
