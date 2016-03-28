(ns voke.clock
  "A system for tracking the amount of sim time (as opposed to wall time) that's elapsed."
  (:require [schema.core :as s])
  (:require-macros [schema.core :as sm]))

(defonce ^:private clock (atom 0))

(defn now [] @clock)

(sm/defn add-time! [dt :- s/Num]
  (swap! clock + dt))
