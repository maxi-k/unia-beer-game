(ns beer-game.components.webserver
  (:require [com.stuartsierra.component :as component]
            [org.httpkit.server :as http-kit]
            [config.core :refer [env]]))

(defrecord WebserverComponent [option-map handler]
  component/Lifecycle

  (start [component]
    (let [handler-fn (if (fn? handler)
                       handler
                       (:handler handler))
          server (http-kit/run-server handler-fn option-map)]
      (assoc component :server server)))

  (stop [component]
    (dissoc component :server)))

(defn new-webserver
  "Creates a new webserver component instance."
  ([] (new-webserver {}))
  ([option-map]
   (map->WebserverComponent
    {:option-map (merge {:port (Integer/parseInt (or (:port env) "3000"))}
                        option-map)})))
