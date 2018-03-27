(ns beer-game.routes
  (:require-macros [secretary.core :refer [defroute]])
  (:import goog.History)
  (:require [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [re-frame.core :as rf]
            [beer-game.config :as config]))

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
    (defroute "/"           [] (set-panel :default-panel))
    (defroute "/overview"   [] (set-panel :overview-panel))
    (defroute "/events"     [] (set-panel :events-panel))
    (defroute "/game-data"  [] (set-panel :game-data-panel))
    (defroute "/statistics" [] (set-panel :statistics-panel))
    (defroute "/imprint"    [] (set-panel :imprint-panel))

    (when config/development?
      (defroute "/devcards" [] (set-panel :devcards-panel)))
    ;; -----------------------------
    (hook-browser-navigation!)))
