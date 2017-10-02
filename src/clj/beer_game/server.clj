(ns beer-game.server
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
  "The system map for the server side."
  []
  (component/system-map
   :websocket (new-websocket get-sch-adapter message-handler)
   :routes    (component/using (new-routes)    [:websocket])
   :handler   (component/using (new-handler)   [:routes])
   :webserver (component/using (new-webserver) [:handler])))

(defn -main
  "Entrypoint for the production system."
  [& args]
  (component/start (server-system)))
