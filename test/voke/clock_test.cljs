(ns voke.clock-test
  (:require [cljs.spec.test :as stest]
            [voke.clock :as clock]))

(stest/check `clock/add-time!)
