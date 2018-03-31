(ns beer-game.core
  "The core namespace for initial setup of the client-side
  code and structure."
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [re-frisk.core :refer [enable-re-frisk!]]
            [beer-game.events] ;; Initialize re-frame event handlers
            [beer-game.subs]   ;; Initialize re-frame subs
            [beer-game.listeners :as listeners]
            [beer-game.routes :as routes]
            [beer-game.view :as view]
            [beer-game.client :as client]
            [beer-game.config :as config]))


(defn dev-setup
  "Sets up the development environment
  if the app is not running in production."
  []
  (when config/development?
    (enable-console-print!)
    (enable-re-frisk!)
    (println "dev mode")))

(defn mount-root
  "Mounts the root-component of the app in the
  document element with id \"app\"."
  []
  (re-frame/clear-subscription-cache!)
  (reagent/render [view/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init
  "Called from the html-page-source. Initializes the beer-game app."
  []
  (routes/app-routes)
  (re-frame/dispatch-sync [:initialize-db])
  (dev-setup)
  (client/init-websocket)
  (listeners/init-listeners)
  (mount-root))
