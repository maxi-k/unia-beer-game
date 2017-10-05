(ns beer-game.routes
  (:require-macros [secretary.core :refer [defroute]])
  (:import goog.History)
  (:require [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [re-frame.core :as rf]))

(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(defn app-routes []
  (secretary/set-config! :prefix "#")
  (letfn [(set-panel [name]
            (rf/dispatch [:set-active-panel name]))]
    ;; ---------- Routes -----------
    ;; The default route
    (defroute "/"           [] (set-panel :overview-panel))
    (defroute "/overview"   [] (set-panel :overview-panel))
    (defroute "/statistics" [] (set-panel :statistics-panel))
    (defroute "/events"     [] (set-panel :events-panel))
    ;; -----------------------------
    (hook-browser-navigation!)))
