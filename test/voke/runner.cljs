(ns voke.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [voke.system.collision-test]
            [voke.state-test]
            [voke.util-test]))

(doo-tests 'voke.system.collision-test
           'voke.state-test
           'voke.util-test)
