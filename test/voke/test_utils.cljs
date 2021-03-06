(ns voke.test-utils
  (:require [cljs.spec :as s]
            [clojure.test.check.generators]
            [voke.specs]
            [voke.state :as state]))

(def blank-game-state (state/make-game-state []))

(def game-state-with-an-entity (assoc-in blank-game-state
                                         [:game-state/entities 0]
                                         (assoc (first (first (s/exercise :entity/entity 1)))
                                                :entity/id
                                                0)))
