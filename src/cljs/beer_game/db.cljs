(ns beer-game.db
  "Namespace for the client-side data-store setup.
  Does noot actually initialize the re-frame store."
  (:require [beer-game.config :as config]))

(def default-db
  "Defines default data that is stored in the
  client-side data store from the beginiing"
  {:name "Beer Game"
   :client {:theme (if config/development? :dark :light)
            :connected false}
   :test {}
   :user {:auth false}
   :messages {}
   :events {}
   :game-state {}
   :selected-event nil})
