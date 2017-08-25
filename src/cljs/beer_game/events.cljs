(ns beer-game.events
  (:require [re-frame.core :as re-frame]
            [beer-game.db :as db]))

(re-frame/reg-event-db
 :initialize-db
 (fn  [_ _]
   db/default-db))
