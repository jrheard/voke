(ns voke.core
  (:require [goog.events :as events]
            [voke.events :refer [make-pub subscribe-to-event]]
            [voke.schemas :refer [GameState]]
            [voke.system.core :refer [run-systems]])
  (:import [goog.events KeyCodes])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [schema.core :as sm])
  (:use [cljs.core.async :only [chan <! put!]]))

(def player {:id                      1
             :position                {:x 488
                                       :y 300}
             :collision-box           {:width 25 :height 25}
             :render-info             {:shape :square}
             :human-controlled        true
             :intended-move-direction #{}
             :intended-fire-direction #{}})

; TODO move keyboard handling code to input.cljs
; and have core expose some functions for modifying the player's intended directions

(def move-key-mappings {KeyCodes.W :up
                        KeyCodes.A :left
                        KeyCodes.S :down
                        KeyCodes.D :right})
(def fire-key-mappings {KeyCodes.DOWN  :down
                        KeyCodes.LEFT  :left
                        KeyCodes.UP    :up
                        KeyCodes.RIGHT :right})
(def key-mappings (merge move-key-mappings fire-key-mappings))

(defn listen-to-keyboard-inputs [event-chan]
  (events/removeAll (.-body js/document))

  (doseq [[goog-event-type event-type-suffix] [[(.-KEYDOWN events/EventType) "down"]
                                               [(.-KEYUP events/EventType) "up"]]]

    (events/listen
      (.-body js/document)
      goog-event-type
      (fn [event]
        (let [code (.-keyCode event)]
          (when (contains? key-mappings code)
            (.preventDefault event)
            (let [event-type (cond
                               (contains? move-key-mappings code) (keyword (str "move-key-" event-type-suffix))
                               (contains? fire-key-mappings code) (keyword (str "fire-key-" event-type-suffix)))]
              (put! event-chan {:type      event-type
                                :direction (key-mappings code)}))))))))

(sm/defn update-player [state :- GameState & args]
  ; TODO tear down, rethink API, reimplement
  (apply update-in
         state
         (concat [:entities 1] (first args))
         (rest args)))

(defn handle-events [state event-chan]
  (listen-to-keyboard-inputs event-chan)

  (go-loop []
    (let [msg (<! event-chan)]
      (js/console.log (clj->js msg))
      (case (msg :type)
        :move-key-down (swap! state update-player [:intended-move-direction] conj (msg :direction))
        :move-key-up (swap! state update-player [:intended-move-direction] disj (msg :direction))
        :fire-key-down (swap! state update-player [:intended-fire-direction] conj (msg :direction))
        :fire-key-up (swap! state update-player [:intended-fire-direction] disj (msg :direction)))
      (recur))))

; TODO :mode? :active-level?
(defonce game-state (atom {:entities {1 player}}))

; Useful in dev, so figwheel doesn't cause a jillion ticks of the system to happen at once
(defonce animation-frame-request-id (atom nil))

(defn ^:export main []
  (js/window.cancelAnimationFrame @animation-frame-request-id)

  ;(.profile js/console "hello")
  ;(js/window.setTimeout #(.profileEnd js/console "hello") 5000)
  (let [event-chan (chan)]
    (handle-events game-state event-chan))

  (js/window.requestAnimationFrame (fn process-frame [ts]
                                     (swap! game-state run-systems)
                                     (reset! animation-frame-request-id
                                             (js/window.requestAnimationFrame process-frame)))))

