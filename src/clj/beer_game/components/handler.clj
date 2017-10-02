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

(defrecord HandlerComponent [development? def-handler? routes]
  component/Lifecycle

  (start [component]
    (let [router-fn (if (fn? routes) routes (:routes routes))
          lol (println router-fn)
          handler-fn (if development?
                       (dev-handler router-fn)
                       (prod-handler router-fn))]
      (if def-handler? (def handler-fn handler-fn))
      (assoc component :handler handler-fn)
      (assoc component :handler-fn `handler-fn)))

  (stop [component]
    (dissoc component :handler)
    (dissoc component :handler-fn)))

(defn new-handler
  "Creates a new handler component instance."
  ([] (new-handler false))
  ([def-handler]
   (map->HandlerComponent {:development? config/development?
                           :def-handler? def-handler})))
