(ns beer-game.db
  (:require [beer-game.config :as config]))

(def default-db
  {:name "re-frame"
   :client {:theme (if config/debug? :dark :light)}})
