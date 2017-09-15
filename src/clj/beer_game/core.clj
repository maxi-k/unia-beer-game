(ns beer-game.core
  (:require [beer-game.handler :as handler]))

(defn -main [& args]
  (println "Server starting...")
  (handler/start-socket))
