(ns voke.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [voke.system.collision-test]))

(doo-tests 'voke.system.collision-test)
