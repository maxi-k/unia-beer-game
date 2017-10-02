(ns beer-game.handler
  (:require [clojure.data :refer [diff]]
            [compojure.core :refer [GET POST context defroutes]]
            [compojure.route :refer [resources]]
            [ring.util.response :refer [resource-response]]
            [ring.middleware
             [reload :refer [wrap-reload]]
             [keyword-params :refer [wrap-keyword-params]]
             [params :refer [wrap-params]]]
            ;; Server Adapter for websockets
            [org.httpkit.server :as http-kit]
            [taoensso.sente :as ws] ;; Websockets
            [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]
            [beer-game
             [config :as config]
             [store :as store]
             [util :as util]]
            [beer-game.handlers
             [auth :as auth]
             [game :as game-handler]
             [system :as system-handler]]))

(when config/development?
  (reset! ws/debug-mode?_ true))

(def socket-status
  "Status of the server-socket"
  (atom {:running false}))

(defn start-socket []
  (println "Starting Socket on Server")
  (defonce channel-socket
    (ws/make-channel-socket! (get-sch-adapter)
                             {:protocol (if config/development? :http :https)
                              :packer config/websocket-packer
                              :user-id-fn :client-id})) ;; request-uid == client-id

  (let [{:keys [ch-recv send-fn connected-uids
                ajax-post-fn ajax-get-or-ws-handshake-fn]} channel-socket]
    (def ring-ajax-post                ajax-post-fn)
    (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
    (def incoming                      ch-recv) ;; ChannelSocket's receive channel
    (def send!                         send-fn) ;; ChannelSocket's send API fn
    (def connected-uids                connected-uids)) ;; Watchable, read-only atom

  (defn broadcast
    "Sends given `message` to all connected uids.
    If a second argument is given, broadcasts it only to those listed in `uids`."
    ([message] (broadcast message (:any @connected-uids)))
    ([message uids] (doseq [uid uids] (send! uid message))))

  (defn msg->inbox
    "Converts an incoming channel message to an 'inbox-message',
    which can then be dispatched to the internal handlers."
    [ev-msg]
    (merge ev-msg {:internal-id (-> ev-msg :id util/name-only)}))

  (defn outbox!
    "Takes the original (incoming) message and the outgoing message
  of the format {:type #{reply broadcast noop} :message message-vector}
  and sends it "
    [{:keys [uid ?reply-fn] :as ev-msg}
     {:keys [type message internal-id] :as internal-msg}]
    (condp = type
      :reply (send! (or (:uid internal-msg) uid) message)
      :reply-fn ((?reply-fn ev-msg) message)
      :user (broadcast message (store/user-id->client-id (or (:uid internal-msg)
                                                             internal-id)))
      :broadcast (broadcast message (or (:uids internal-msg)
                                        (:any @connected-uids)))
      :noop "Don't reply to anyone."
      (println "Unhandled internal message: " internal-msg " from incoming message: " ev-msg)))

  (defmulti event
    "Dispatch on the id of the event message being sent to the socket,
    by splitting the namespaced id into a vector and dispatching on that."
    (fn [msg]
      (-> msg :id namespace keyword)))

  ;; Handles all authentication messages
  (defmethod event :auth
    [ev-msg]
    (outbox! ev-msg
             (-> ev-msg
                 msg->inbox
                 auth/handle-msg)))

  ;; Handles all game messages
  ;; Secure channel (user has to be logged in)
  (defmethod event :game
    [{:keys [client-id] :as ev-msg}]
    (if (auth/logged-in? client-id)
      (outbox!
       ev-msg
       (-> ev-msg
           msg->inbox
           game-handler/handle-msg))
      (send! client-id [:auth/unauthorized])))

  ;; Handles all system messages (chsk)
  (defmethod event :chsk
    [ev-msg]
    (outbox! ev-msg (-> ev-msg
                        msg->inbox
                        system-handler/handle-msg)))

  (defmethod event :testing
    [{:as ev-msg :keys [event ?data uid]}]
    (send! uid [:testing/echo ?data]))

  (defmethod event :default
    [{:keys [event]}]
    (println "Unhandled event: " event))

  (defn event-handler
    [msg]
    (event msg))

  (defonce router (ws/start-server-chsk-router! incoming event-handler))

  (defroutes routes
    ;; Websocket endpoints
    (GET config/websocket-endpoint req (ring-ajax-get-or-ws-handshake req))
    (POST config/websocket-endpoint req (ring-ajax-post               req))
    ;; Default index.html endpoint
    (GET "/" [] (resource-response "index.html" {:root "public"}))
    ;; Other resources (css, js, img)
    (resources "/"))

  (def handler-core
    "A common handler for both development and production environments."
    (-> #'routes
        wrap-keyword-params
        wrap-params))

  (def dev-handler (-> handler-core wrap-reload))
  (def handler handler-core)
  ;; Mark the socket as started
  (swap! socket-status assoc :running true))

;; When in development, start the socket if it is not running already
(when (and config/development?
           (:running @socket-status))
  (start-socket))
