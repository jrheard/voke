(ns voke.clock
  "A system for tracking the amount of sim time (as opposed to wall time) that's elapsed."
  (:require [cljs.spec :as s]))

(defonce ^:private clock (atom 0))

(defn now [] @clock)

(defn add-time! [dt]
  (swap! clock + dt))

(s/fdef add-time!
  :args (s/cat :dt number?))
