(ns voke.test-utils
  (:require [voke.state :as state]))

(def blank-game-state (state/make-game-state []))
(def game-state-with-an-entity (assoc-in blank-game-state
                                         [:entities 0]
                                         {:id 0 :renderable false}))

(defn truthy? [x]
  (if x true false))

(defn falsy? [x]
  (if x false true))
