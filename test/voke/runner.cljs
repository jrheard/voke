(ns voke.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [voke.input]
            [voke.clock-test]
            [voke.input-test]
            [voke.events-test]
            [voke.system.collision-test]
            [voke.system.core-test]
            [voke.system.movement-test]
            [voke.state-test]
            [voke.util-test]))

(doo-tests 'voke.clock-test
           'voke.input-test
           'voke.events-test
           'voke.system.collision-test
           'voke.system.core-test
           'voke.system.movement-test
           'voke.state-test
           'voke.util-test)
