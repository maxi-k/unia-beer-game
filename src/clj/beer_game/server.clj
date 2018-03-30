(ns beer-game.server
  "Main server namespace. Defines the java main function, which
  starts up a `com.stuartsierra.component` system map that defines the
  main server architecture using the lifecycle abstraction, and puts the
  single components together using dependency injection."
  (:require [com.stuartsierra.component :as component]
            [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]
            [beer-game.message-handler :refer [message-handler]]
            [beer-game.components
             [webserver :refer [new-webserver]]
             [websocket :refer [new-websocket]]
             [routes :refer [new-routes]]
             [handler :refer [new-handler]]])
  (:gen-class))

(defn server-system
  "The system map for the server side, which defines the structure
  of the server system and handles starting and stopping using the
  lifecycle abstraction. Puts the single components together using
  dependency injection."
  []
  (component/system-map
   :websocket (new-websocket get-sch-adapter message-handler)
   :routes    (component/using (new-routes)    [:websocket])
   :handler   (component/using (new-handler)   [:routes])
   :webserver (component/using (new-webserver) [:handler])))

(defn -main
  "Entrypoint for the production system. Starts the [[server-system]]."
  [& args]
  (component/start (server-system)))
