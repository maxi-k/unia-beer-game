(ns beer-game.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [re-frisk.core :refer [enable-re-frisk!]]
            [beer-game.events] ;; Initialize re-frame event handlers
            [beer-game.subs]   ;; Initialize re-frame subs
            [beer-game.listeners :as listeners]
            [beer-game.routes :as routes]
            [beer-game.views :as views]
            [beer-game.config :as config]))


(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (enable-re-frisk!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (routes/app-routes)
  (re-frame/dispatch-sync [:initialize-db])
  (dev-setup)
  (listeners/init-listeners)
  (mount-root))
