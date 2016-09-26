(ns voke.test-utils
  (:require [voke.state :as state]))

(def blank-game-state (state/make-game-state []))
(def game-state-with-an-entity (assoc-in blank-game-state
                                         [:entities 0]
                                         {:id 0 :renderable false}))

