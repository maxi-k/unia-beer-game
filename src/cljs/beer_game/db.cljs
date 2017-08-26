(ns beer-game.db
  (:require [beer-game.config :as config]))

(def default-db
  {:name "Beer Game"
   :client {:theme (if config/debug? :dark :light)}
   :game {}})
