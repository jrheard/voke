(ns voke.system.core
  (:require [voke.schemas :refer [GameState]]
            [voke.system.movement :refer [move-system]]
            [voke.system.rendering :refer [render-system]] )
  (:require-macros [schema.core :as sm]))

(sm/defn run-systems :- GameState
         [state :- GameState]
         (render-system (:entities state))
         (-> state
             move-system))
