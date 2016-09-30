(ns voke.schemas
  (:require [cljs.spec :as sp]
            [schema.core :as s])
  (:require-macros [schema.core :as sm]))

(sm/defschema EventType (s/enum :movement :entity-added :entity-removed))

; TODO not well defined
; maybe best thing to do would be to schematize each individual event and then say an Event is any of 'em
(sm/defschema Event {:type EventType
                     s/Any s/Any})

(sm/defschema System {(s/optional-key :tick-fn)        s/Any
                      (s/optional-key :initialize)     s/Any
                      (s/optional-key :event-handlers) [{:event-type EventType
                                                         :fn         s/Any}]})
