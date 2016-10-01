(ns voke.runner
  (:require [cljs.spec.test :as stest]
            [doo.runner :refer-macros [doo-tests]]
            [voke.input]
            [voke.clock-test]
            [voke.input-test]
            [voke.events-test]
            [voke.system.collision-test]
            [voke.system.core-test]
            [voke.system.movement-test]
            [voke.state-test]
            [voke.util-test]))

(stest/instrument `voke.input/remove-conflicting-directions)


(doo-tests 'voke.clock-test
           'voke.input-test
           'voke.events-test
           'voke.system.collision-test
           'voke.system.core-test
           'voke.system.movement-test
           'voke.state-test
           'voke.util-test)
