(ns voke.schemas
  (:require [cljs.spec :as sp]
            [schema.core :as s])
  (:require-macros [schema.core :as sm]))

(sm/defschema System {(s/optional-key :tick-fn)        s/Any
                      (s/optional-key :initialize)     s/Any
                      (s/optional-key :event-handlers) [{:event-type EventType
                                                         :fn         s/Any}]})
