(ns beer-game.config
  #?(:cljs
     (:require-macros [beer-game.server-utils :refer [read-resource]]))
  #?(:clj (:require [config.core :refer [env]])))


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
  {:public-root "public"
   :websocket-endpoint "/ws"
   :websocket-packer :edn
   :realms {:realm/player {:title "Mitspieler"}
            :realm/leader {:title "Spielleiter"}}
   :user-roles {:role/brewery {:title "Brauerei"}
                :role/big-market {:title "Großhandel"}
                :role/small-market {:title "Kleinhandel"}
                :role/customer {:title "Kunde"}}})

(def websocket-endpoint
  "The relative url of the websocket endpoint."
  (:websocket-endpoint definitions))

(def websocket-packer
  "The packer used for websocket communication."
  (:websocket-packer definitions))

(def public-root
  (:public-root definitions))

(def realms (definitions :realms))
(def player-realm :realm/player)
(def leader-realm :realm/leader)
(def player-realm-data (-> player-realm :realms player-realm))
(def leader-realm-data (-> leader-realm :realms leader-realm))

(def user-roles (definitions :user-roles))
(def allowed-user-roles (-> definitions :user-roles keys set))

#?(:clj
   ;; Server Side Configuration
   (do
     (def server-config
       "The config file for admins to edit.
  Should only be visible on the server."
       (read-string (slurp "server-config.edn")))

     (def leader-password
       "The password for the game leader."
       (:leader-password server-config))))
