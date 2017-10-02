(ns beer-game.handlers.system
  (:require [beer-game.store :as store]))

(defn handle-msg
  "Handles all system-messages."
  [{:as ev-msg :keys [internal-id client-id]}]
  (println internal-id)
  (condp = internal-id
    :ws-ping (do (println "Ping from client: " client-id) {:type :noop})
    :uidport-open (do (store/add-client! client-id) {:type :noop})
    :uidport-close (do (store/remove-client! client-id) {:type :noop})
    {:type ::unhandled}))
