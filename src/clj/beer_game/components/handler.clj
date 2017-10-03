(ns beer-game.components.handler
  (:require
   [com.stuartsierra.component :as component]
   [ring.middleware
    [reload :refer [wrap-reload]]
    [keyword-params :refer [wrap-keyword-params]]
    [params :refer [wrap-params]]]
   [beer-game.config :as config]))

(defn- handler-core
  "A common handler for both development and production environments,
  wrapped as a closure over the routes."
  [routes]
  (-> routes
      wrap-keyword-params
      wrap-params))

(defn- dev-handler
  "Handler function for the development environment."
  [routes]
  (-> routes handler-core wrap-reload))

(defn- prod-handler
  "Handler function for production."
  [routes]
  (handler-core routes))

(defrecord HandlerComponent [development? routes]
  component/Lifecycle

  (start [component]
    (let [router-fn (if (fn? routes) routes (:routes routes))
          handler-fn (if development?
                       (dev-handler router-fn)
                       (prod-handler router-fn))]
      (assoc component :handler handler-fn)))

  (stop [component]
    (dissoc component :handler)))

(defn new-handler
  "Creates a new handler component instance."
  []
  (map->HandlerComponent {:development? config/development?}))
