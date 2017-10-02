(ns user
  (:use beer-game.dev-main)
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :as repl]))

(defn reset []
  (stop)
  (repl/refresh :after 'beer-game.dev-main/go))
