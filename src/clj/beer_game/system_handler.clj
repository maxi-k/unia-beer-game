(ns beer-game.system-handler)

(defn handle-msg
  "Handles all system-messages"
  [{:as ev-msg :keys [internal-id client-id]}]
  (condp = internal-id
    :ws-ping (println "Ping from client: " client-id)
    {:type :noop}))
