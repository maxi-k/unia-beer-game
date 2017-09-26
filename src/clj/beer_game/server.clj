(ns beer-game.server
  (:require [beer-game.handler :refer [handler]]
            [beer-game.config :as config]
            [config.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]]
            [org.httpkit.server :as http-kit]
            [beer-game.handler :as handler])
  (:gen-class))

 (defn -main [& args]
   (let [port (Integer/parseInt (or (env :port) "3000"))]
     (if config/development?
       (run-jetty handler {:port port :join? false})
       (do
         (handler/start-socket)
         (http-kit/run-server handler {:port port})))))
