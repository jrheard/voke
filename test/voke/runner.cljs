(ns voke.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [voke.input-test]
            [voke.system.collision-test]
            [voke.state-test]
            [voke.util-test]))

(doo-tests 'voke.input-test
           'voke.system.collision-test
           'voke.state-test
           'voke.util-test)
