(ns beer-game.config
  #?(:cljs
     (:require-macros [beer-game.server-utils :refer [read-resource]]))
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
   :realms {:player {:title "Mitspieler"}
            :leader {:title "Spielleiter"}}
   :user-ids {:brewery {:title "Brauerei"}
              :big-market {:title "Großhandel"}
              :small-market {:title "Kleinhandel"}
              :customer {:title "Kunde"}}})

(def websocket-endpoint
  "The relative url of the websocket endpoint."
  (:websocket-endpoint definitions))

(def websocket-packer
  "The packer used for websocket communication."
  (:websocket-packer definitions))

(def realms (definitions :realms))
(def player-realm :player)
(def leader-realm :leader)
(def player-realm-data (-> player-realm :realms player-realm))
(def leader-realm-data (-> leader-realm :realms leader-realm))

(def user-ids (definitions :user-ids))
(def allowed-user-ids (-> definitions :user-ids keys set))

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
