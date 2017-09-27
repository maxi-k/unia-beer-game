(ns beer-game.handler
  (:require [clojure.data :refer [diff]]
            [compojure.core :refer [GET POST context defroutes]]
            [compojure.route :refer [resources]]
            [ring.util.response :refer [resource-response]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [taoensso.sente :as ws] ;; Websockets
            ;; Server Adapter for websockets
            [org.httpkit.server :as http-kit]
            [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]
            [beer-game.config :as config]
            [beer-game.store :as store]
            [beer-game.util :as util]
            [beer-game.auth :as auth]
            [beer-game.game-handler :as gh]))

(when config/development?
  (reset! ws/debug-mode?_ true))

(defn start-socket []
  (println "Starting Socket on Server")
  (defonce channel-socket
    (ws/make-channel-socket! (get-sch-adapter)
                             {:protocol (if config/development? :http :https)
                              :packer config/websocket-packer
                              :user-id-fn store/request->uid!}))

  (let [{:keys [ch-recv send-fn connected-uids
                ajax-post-fn ajax-get-or-ws-handshake-fn]} channel-socket]
    (def ring-ajax-post                ajax-post-fn)
    (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
    (def incoming                      ch-recv) ;; ChannelSocket's receive channel
    (def send!                         send-fn) ;; ChannelSocket's send API fn
    (def connected-uids                connected-uids)) ;; Watchable, read-only atom

  ;; Remove entries from the connected-uids that
  #_(add-watch connected-uids :track-disconnects
             (fn [_ _ old new]
               ;; diff returns a vector [only-a only-b both]
               (let [[only-old _ _] (diff (:any old) (:any new))]
                 (when-not (empty? only-old)
                   (store/remove-client-ids! only-old)))))

  (defn broadcast
    "Sends given `message` to all connected uids.
    If a second argument is given, broadcasts it only to those listed in `uids`."
    ([message] (broadcast message (:any @connected-uids)))
    ([message uids] (doseq [uid uids] (send! uid message))))

  (defn outbox!
    "Takes the original (incoming) message and the outgoing message
  of the format {:type #{reply broadcast noop} :message message-vector}
  and sends it "
    [{:keys [uid] :as ev-msg}
     {:keys [type message] :as internal-msg}]
    (condp = type
      :reply (send! (or (:uid internal-msg) uid) message)
      :broadcast (broadcast message (or (:uids internal-msg) (:any @connected-uids)))
      :noop "Don't reply to anyone."
      (println "Unhandled internal message: " internal-msg " from incoming message: " ev-msg)))

  (defmulti event
    "Dispatch on the id of the event message being sent to the socket,
    by splitting the namespaced id into a vector and dispatching on that."
    (fn [msg]
      (-> msg :id util/split-keyword)))

  (defmethod event :default
    [{:keys [event]}]
    (println "Unhandled event: " event))

  (defmethod event [:testing :echo]
    [{:as ev-msg :keys [event ?data uid]}]
    (send! uid [:testing/echo ?data]))

  (defmethod event [:auth :login]
    [{:as ev-msg :keys [client-id ?data]}]
    (outbox! ev-msg (auth/authenticate! client-id ?data)))

  (defmethod event [:auth :logout]
    [{:keys [client-id] :as ev-msg}]
    (outbox! ev-msg (auth/logout! client-id)))

  (defmethod event [:game ::default]
    [{:keys [client-id] :as ev-msg}]
    (if (auth/logged-in? client-id)
      (outbox!
       ev-msg
       (gh/handle-msg (update ev-msg :id util/name-only)))
      (send! client-id [:auth/unauthorized]) ))

  (defmethod event [:chsk :ws-ping] [_])

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
  )

(when config/development? (start-socket))
