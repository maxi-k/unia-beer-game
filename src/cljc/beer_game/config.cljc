(ns beer-game.config
  #?(:cljs
     (:require-macros [beer-game.macros :refer [read-resource]]))
  (:require #?(:clj [config.core :refer [env]])))


;; On the client, yogthos/config is not available.
;; Thus, just read the config file
#?(:cljs (def env (read-resource "config.edn")))

(def environment
  "Returns all environment variables in the config."
  (:environment env))

(def development?
  "Returns true if we are in an development environment."
  (:development environment))

(def dev?
  "Shortcut for development?"
  development?)

(def debug?
  #?(:cljs ^boolean goog.DEBUG
     :clj development?))

(def definitions
  "Returns the common config shared among all environments."
  {:websocket-endpoint "/ws"
   :websocket-packer :edn
   :auth
   {:player-realm :player
    :leader-realm :leader
    :allowed-user-ids #{:brewery :big-market :small-market :customer}}})

(def websocket-endpoint
  "The relative url of the websocket endpoint."
  (:websocket-endpoint definitions))

(def websocket-packer
  "The packer used for websocket communication."
  (:websocket-packer definitions))

(def auth-config
  (:auth definitions))

#?(:clj (def leader-password
          "The password for the game leader."
          "testpasswort"))
