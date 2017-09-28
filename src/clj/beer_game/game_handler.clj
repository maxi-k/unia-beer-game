(ns beer-game.game-handler)

(defmulti handle-msg
  "Dispatches on all game events."
  :id)

(defmethod handle-msg
  :next-round
  [{:as msg}]
  {:type :broadcast
   :message [:game/next-round]})

(defmethod handle-msg :default
  [msg]
  (println "Unhandled game message: " msg))
