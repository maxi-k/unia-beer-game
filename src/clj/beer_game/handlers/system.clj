(ns beer-game.handlers.system
  (:require [beer-game.store :as store]))

(defn handle-msg
  "Handles all system-messages."
  [{:as ev-msg :keys [internal-id client-id]}]
  (condp = internal-id
    :ws-ping {:type :noop}
    :uidport-open {:type :noop}
    :uidport-close {:type :noop}
    {:type ::unhandled}))
