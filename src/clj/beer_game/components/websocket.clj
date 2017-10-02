(ns beer-game.components.websocket
  (:require [com.stuartsierra.component :as component]
            [taoensso.sente :as ws]
            [beer-game.config :as config]))


(defrecord WebsocketServer [development? packer adapter-fn message-handler]
  component/Lifecycle

  (start [component]
    (let [socket (ws/make-channel-socket!
                  (adapter-fn)
                  {:protocol (if development? :http :https)
                   :packer packer
                   ;; request-uid == client-id
                   :user-id-fn :client-id})
          {:keys [ch-recv send-fn connected-uids
                  ajax-post-fn ajax-get-or-ws-handshake-fn]} socket
          router (ws/start-server-chsk-router!
                  ch-recv
                  ;; Pass the component to the message-handler-creator
                  (message-handler component))]

      (-> component
          (assoc :socket socket)
          (assoc :router router)
          (assoc :ring-ajax-post                ajax-post-fn)
          (assoc :ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
          (assoc :incoming                      ch-recv) ;; ChannelSocket's receive channel
          (assoc :send!                         send-fn) ;; ChannelSocket's send API fn
          (assoc :connected-uids                connected-uids))))

  (stop [component]
    (-> component
        (dissoc :socket)
        (dissoc :router)
        (dissoc :ring-ajax-post)
        (dissoc :ring-ajax-get-or-ws-handshake)
        (dissoc :incoming) ;; ChannelSocket's receive channel
        (dissoc :send!) ;; ChannelSocket's send API fn
        (dissoc :connected-uids))))

(defn new-websocket
  "Creates a new websocket component instance."
  [adapter-fn message-handler]
  (map->WebsocketServer {:development? config/development?
                         :packer config/websocket-packer
                         :adapter-fn adapter-fn
                         :message-handler message-handler}))
