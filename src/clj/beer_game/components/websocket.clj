(ns beer-game.components.websocket
  "Defines the `component` for the websocket used in the server
  system (see [[beer-game.server/server-system]]), the routes of which
  are defined in [[beer-game.components.routes]]."
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
          new-comp
          (assoc component
                 :socket                        socket
                 :ring-ajax-post                ajax-post-fn
                 :ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn
                 :incoming                      ch-recv ;; ChannelSocket's receive channel
                 :send!                         send-fn ;; ChannelSocket's send API fn
                 :connected-uids                connected-uids)
          msg-handler (message-handler new-comp)
          router (ws/start-server-chsk-router!
                  ch-recv
                  ;; Pass the component to the message-handler-creator
                  msg-handler)]
      (assoc new-comp :router router)))

  (stop [component]
    (dissoc component
            :socket
            :router
            :ring-ajax-post
            :ring-ajax-get-or-ws-handshake
            :incoming ;; ChannelSocket's receive channel
            :send! ;; ChannelSocket's send API fn
            :connected-uids)))

(defn new-websocket
  "Creates a new websocket component instance."
  [adapter-fn message-handler]
  (map->WebsocketServer {:development? config/development?
                         :packer config/websocket-packer
                         :adapter-fn adapter-fn
                         :message-handler message-handler}))
