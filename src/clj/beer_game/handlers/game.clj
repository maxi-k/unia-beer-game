(ns beer-game.handlers.game)

(defmulti handle-game-msg
  "Dispatches on all game events."
  :internal-id)

(defmethod handle-game-msg
  :next-round
  [{:as msg}]
  {:type :broadcast
   :message [:game/next-round]})

(defmethod handle-game-msg :default
  [msg]
  {:type ::unhandled})
