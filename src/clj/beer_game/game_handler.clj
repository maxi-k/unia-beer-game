(ns beer-game.game-handler)

(defmulti handle-msg
  "Dispatches on all game events."
  :id)

(defmethod handle-msg)
