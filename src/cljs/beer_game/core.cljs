(ns beer-game.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [beer-game.events]
            [beer-game.subs]
            [beer-game.views :as views]
            [beer-game.config :as config]))


(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (dev-setup)
  (mount-root))
