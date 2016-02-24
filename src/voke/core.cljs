(ns voke.core
  (:require [schema.core :as s]
            [goog.events :as events])
  (:import [goog.events KeyCodes])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [schema.core :as sm])
  (:use [cljs.core.async :only [chan <! >! put! timeout]]))

(sm/defschema MoveDirection (s/enum :up :right :down :left))
(sm/defschema IntendedMoveDirection #{MoveDirection})

(sm/defschema Entity {:id                         s/Int
                      (s/maybe :position)         {:x s/Num
                                                   :y s/Num}
                      (s/maybe :collision-box)    {:width  s/Int
                                                   :height s/Int}
                      (s/maybe :render-info)      {:shape (s/enum :square)}
                      (s/maybe :human-controlled) {:indended-move-direction IntendedMoveDirection
                                                   :intended-fire-direction s/Any}})

(def player {:id            1
             :position      {:x 10
                             :y 10}
             :collision-box {:width 50 :height 50}
             :render-info   {:shape :square}
             :human-controlled {:intended-move-direction #{}}
             })

; TODO :mode? :active-level?
(def game-state (atom {:entities [player]}))

(sm/defn render-system
  [entities :- [Entity]]
  ; TODO  get a good canvas library via cljsjs, fabric or phaser seem to be top contenders, research
  (let [canvas (js/document.getElementById "screen")
        ctx (.getContext canvas "2d")]
    (aset ctx "fillStyle" "rgb(50,50,50)")
    (doseq [entity entities]
      (.fillRect ctx
                 (-> entity :position :x)
                 (-> entity :position :y)
                 (-> entity :collision-box :width)
                 (-> entity :collision-box :height)))))

(def move-keys #{KeyCodes.DOWN KeyCodes.LEFT KeyCodes.UP KeyCodes.RIGHT})

(defn listen-to-keyboard-inputs [event-chan]
  (events/removeAll (.-body js/document))
  (events/listen
    (.-body js/document)
    (.-KEYDOWN events/EventType)
    (fn [event]
      (let [code (.-keyCode event)]
        (when (contains? move-keys code)
          (.preventDefault event)
          (put! event-chan {:type      :move-key-down
                            :direction ({KeyCodes.DOWN  :down
                                         KeyCodes.UP    :up
                                         KeyCodes.LEFT  :left
                                         KeyCodes.RIGHT :right} code)})))))
  ; TODO DRY
  (events/listen
    (.-body js/document)
    (.-KEYUP events/EventType)
    (fn [event]
      (let [code (.-keyCode event)]
        (when (contains? move-keys code)
          (.preventDefault event)
          (put! event-chan {:type      :move-key-up
                            :direction ({KeyCodes.DOWN  :down
                                         KeyCodes.UP    :up
                                         KeyCodes.LEFT  :left
                                         KeyCodes.RIGHT :right} code)}))))))

(defn handle-events [state event-chan]
  (listen-to-keyboard-inputs event-chan)

  (go-loop []
    (let [msg (<! event-chan)]
      (js/console.log (clj->js msg))
      (case (msg :type)
        ; TODO function for getting the player
        :move-key-down (swap! state update-in [:entities 0 :human-controlled :intended-move-direction] conj (msg :direction))
        :move-key-up (swap! state update-in [:entities 0 :human-controlled :intended-move-direction] disj (msg :direction))
        )
      (js/console.log (clj->js (get-in @state [:entities 0 :human-controlled :intended-move-direction])))
      (recur))))

(defn ^:export main []
  (render-system (:entities @game-state))
  (let [event-chan (chan)]
    (handle-events game-state event-chan))
  )

