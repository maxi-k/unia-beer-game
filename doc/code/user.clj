(ns user
  "The user namespace. Used only for development and the REPL."
  (:use system.repl)
  (:require
   [beer-game.store :as store]
   [beer-game.spec.game :as game-spec]
   [beer-game.config :as config])
  #_(:require [reloaded.repl :refer [system init start stop go reset reset-all]]))

#_(reloaded.repl/set-init! server-system)

(defn create-test-event!
  "Creates a test event with the event-id TEST."
  []
  (store/create-event! {:event/id "TEST"
                        :event/name "Test Event"
                        :game/data {:game/settings config/default-game-settings}}))

(defn destroy-test-event!
  "Destroyes the event with the event-id TEST."
  []
  (store/destroy-event! "TEST"))

(defn start-test-event!
  "Starts the event with the event-id TEST."
  []
  (store/start-event! "TEST"))

(defn recreate-test-event!
  "Destroys the existing test event and then creates a new one."
  []
  (destroy-test-event!)
  (create-test-event!))
