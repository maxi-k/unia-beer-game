(ns beer-game.handlers.game
  (:require [beer-game.store :as store]))

(defmulti handle-game-msg
  "Dispatches on all game events."
  :internal-id)

(defmethod handle-game-msg
  :round-actions
  [{:as msg}]
  {:type :broadcast
   :message [:game/next-round]})

(defmethod handle-game-msg :default
  [msg]
  {:type ::unhandled})
