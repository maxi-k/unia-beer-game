(ns beer-game.client
  (:require [taoensso.sente  :as ws :refer (cb-success?)]
            [beer-game.config :as config]
            [beer-game.api :as api]))

(defn init-websocket []
  ;; Set up websocket communication
  (defonce channel-socket
    (ws/make-channel-socket! config/websocket-endpoint
                             ;; e/o #{:auto :ajax :ws}
                             {:type :auto}))
  (let [{:keys [chsk ch-recv send-fn state]} channel-socket]
    (def chsk      chsk)
    (def incoming  ch-recv) ; ChannelSocket's receive channel
    (def send!     send-fn) ; ChannelSocket's send API fn
    (def msg-state state))  ; Watchable, read-only atom

  (defmulti message-handler :id)

  (defmethod message-handler :default [{:as ev-msg :keys [event]}]
    (println "Unhandled event: " event))

  (defmethod message-handler :chsk/recv [{:as ev-msg :keys [event]}]
    (let [data (event 1)]
      (api/dispatch-server-event {:id (data 0)
                                     :data (data 1)})))

  (defmethod message-handler :chsk/state [{:as ev-msg :keys [?data]}]
    (if (= ?data {:first-open? true})
      (println "Channel socket successfully established!")
      (println "Channel socket state change:" ?data)))

  (defmethod message-handler :chsk/handshake [{:as ev-msg :keys [?data]}]
    (let [[?uid ?csrf-token ?handshake-data] ?data]
      (println "Handshake:" ?data)))

  (defonce router
    (ws/start-client-chsk-router! incoming message-handler)))
