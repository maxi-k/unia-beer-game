(ns beer-game.client
  "Websocket connector. Handles all messages from- and to the server."
  (:require [taoensso.sente  :as ws :refer (cb-success?)]
            [beer-game.config :as config]
            [beer-game.api :as api]))

(defn init-websocket
  "Set up the websocket communication by dynamically binding
  the necessary functions in this namespace once called."
  []
  (defonce channel-socket
    (ws/make-channel-socket! config/websocket-endpoint
                             ;; e/o #{:auto :ajax :ws}
                             {:type :auto
                              :client-id (str (random-uuid))}))

  (let [{:keys [chsk ch-recv send-fn state]} channel-socket]
    (def chsk      chsk)
    (def incoming  ch-recv) ; ChannelSocket's receive channel
    (def send!     send-fn) ; ChannelSocket's send API fn
    (def msg-state state))  ; Watchable, read-only atom

  #_(if config/dev?
      (add-watch msg-state :log-msg-state
                 (fn [_ _ _ new]
                   (.log js/console new))))

  (add-watch msg-state :connection-update
             (fn [_ _ old new]
               (if-not (= (:open? old) (:open? new))
                 (api/dispatch-server-event {:id :system/connection
                                             :data (:open? new)}))))

  (def ping-key :chsk/ws-ping)

  (defmulti message-handler
    "A multifunction for handling the given message"
    :id)

  (defmethod message-handler :default [{:as ev-msg :keys [event]}]
    (println "Unhandled event: " event))

  (defmethod message-handler :chsk/recv [{:as ev-msg :keys [event state]}]
    (let [data (event 1)]
      (api/dispatch-server-event {:id (nth data 0 :no-id)
                                  :data (nth data 1 nil)})))

  (defmethod message-handler :chsk/state [{:as ev-msg :keys [?data]}]
    (when (= ?data {:first-open? true})
      (println "Channel socket successfully established!")))

  (defmethod message-handler :chsk/handshake [{:as ev-msg :keys [?data]}]
    (let [[?uid ?csrf-token ?handshake-data] ?data]
      (println "Handshake:" ?data)))

  (defn message-handler-wrapper
    "A wrapper for the actual message handler function,
  adding logging and error handling and such."
    [msg]
    (message-handler msg))

  (defonce router
    (ws/start-client-chsk-router! incoming message-handler-wrapper)))
