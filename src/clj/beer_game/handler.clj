(ns beer-game.handler
  (:require [compojure.core :refer [GET context defroutes]]
            [compojure.route :refer [resources]]
            [ring.util.response :refer [resource-response]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.format-params :refer [wrap-json-params]]
            [ring.middleware.format-response :refer [wrap-json-response]]))

(defn response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json"}
   :body data})

(defroutes routes
  (GET "/api/action" [] (response {:awesome :cool}))
  (GET "/" [] (resource-response "index.html" {:root "public"}))
  (resources "/"))

(def handler-core
  "A common handler for both development and production environments."
  (-> #'routes
      wrap-json-params
      wrap-json-response))

(def dev-handler (-> handler-core wrap-reload))
(def handler handler-core)
