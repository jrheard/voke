(ns voke.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [voke.input-test]
            [voke.events-test]
            [voke.system.collision-test]
            [voke.system.core-test]
            [voke.state-test]
            [voke.util-test]))

(doo-tests 'voke.input-test
           'voke.events-test
           'voke.system.collision-test
           'voke.system.core-test
           'voke.state-test
           'voke.util-test)
