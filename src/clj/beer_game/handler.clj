(ns beer-game.handler
  (:require [compojure.core :refer [GET POST context defroutes]]
            [compojure.route :refer [resources]]
            [ring.util.response :refer [resource-response]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [taoensso.sente :as ws] ;; Websockets
            ;; Server Adapter for websockets
            [taoensso.sente.server-adapters.http-kit :refer (get-sch-adapter)]
            [beer-game.config :as config]))

(defn start-socket []
  (defonce channel-socket
    (ws/make-channel-socket! (get-sch-adapter)
                             {:protocol (if config/development? :http :https)
                              :packer config/websocket-packer}))

  (let [{:keys [ch-recv send-fn connected-uids
                ajax-post-fn ajax-get-or-ws-handshake-fn]} channel-socket]
    (def ring-ajax-post                ajax-post-fn)
    (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
    (def incoming                      ch-recv) ;; ChannelSocket's receive channel
    (def send!                         send-fn) ;; ChannelSocket's send API fn
    (def connected-uids                connected-uids)) ;; Watchable, read-only atom


  (defmulti event :id)

  (defmethod event :default [{:keys [event]}]
    (println "Unhandled event: " event))

  (defmethod event :testing/echo [{:as ev-msg :keys [event ?data uid]}]
    (send! uid [:testing/echo {:payload (:payload ?data)}]))

  (defmethod event :chsk/ws-ping [_])

  (defonce router (ws/start-chsk-router! incoming event))

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
(start-socket)
