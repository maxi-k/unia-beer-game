(ns beer-game.components.routes
  "Defines the [[routes]] used by the handler of the webserver
  to accept and answer requests, as well as the `component`
  used in the server system (see [[beer-game.server/server-system]])."
  (:require [com.stuartsierra.component :as component]
            [ring.util.response :refer [resource-response]]
            [compojure
             [core :refer [GET POST]]
             [route :refer [resources]]]
            [beer-game.config :as config]))

(defn- routes
  "Returns the handler-function for the server as a closure over
  the websocket-component."
  [{:as websocket-component
    :keys [ring-ajax-get-or-ws-handshake ring-ajax-post]}]
  (compojure.core/routes
   ;; Websocket endpoints
   (GET  config/websocket-endpoint req (ring-ajax-get-or-ws-handshake req))
   (POST config/websocket-endpoint req (ring-ajax-post                req))
   ;; Default index.html endpoint
   (GET "/" [] (resource-response "index.html" {:root config/public-root}))
   (resources "/public")  ;; Other resources (css, js, img)
   (resources "/")))

(defrecord RoutesComponent [websocket]
  component/Lifecycle
  (start [component]
    (assoc component :routes (routes websocket)))
  (stop [component]
    (dissoc component :routes)))

(defn new-routes
  "Creates a new [[RoutesComponent]] instance."
  []
  (map->RoutesComponent {}))
