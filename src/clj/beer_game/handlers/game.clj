(ns beer-game.handlers.game)

(defmulti handle-msg
  "Dispatches on all game events."
  :internal-id)

(defmethod handle-msg
  :next-round
  [{:as msg}]
  {:type :broadcast
   :message [:game/next-round]})

(defmethod handle-msg :default
  [msg]
  {:type ::unhandled})
