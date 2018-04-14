(ns beer-game.config
  "Various configuration options - defined as a map in [[definitions]] - for
  system-level things like websocket routes and resource paths, as well as
  domain-level things like the default supply chain or route colors.
  This namespace is accessible from both the server and the client, and provides
  the server-side settings as well on the server (from server-config.edn)."
  #?(:cljs
     (:require-macros [beer-game.server-utils :refer [read-resource]]))
  #?(:clj (:require [config.core :refer [env]])))

;; On the client, yogthos/config is not available.
;; Thus, just read the config file
#?(:cljs
   (def env
     "The settings from the `config.edn` files *read at compile time*
  for the client-side clojurescript."
     (read-resource "config.edn")))

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
  "Returns whether we are in debugging mode on the client.
  Is an alias for [[development?]] on the server."
  #?(:cljs ^boolean goog.DEBUG
     :clj development?))

(def definitions
  "Returns the common config shared among all environments."
  {:game-title "Beer Game"
   :public-root "public"
   :game-logo "img/icons/pint.svg"
   :game-area-path "img/game-areas/"
   :icon-path "img/icons/"
   :websocket-endpoint "/ws"
   :websocket-packer :edn
   :realms #:realm{:player {:title "Mitspieler"}
                   :leader {:title "Spielleiter"}}
   :user-roles #:role{:brewery {:title "Brauerei"
                                :color "#FFB200"
                                :icon "brewery"}
                      :distributor {:title "Distributor"
                                    :color "#008EFF"
                                    :icon "distribution"}
                      :big-market {:title "Großhandel"
                                   :color "#2A9300"
                                   :icon "big-market"}
                      :small-market {:title "Kleinhandel"
                                     :color "#B900FF"
                                     :icon "small-market"}
                      :customer {:title "Kunde"
                                 :color "#4A4A4A"
                                 :icon ["customer-male" "customer-female"]
                                 :except #{:realm/player}}}
   :supply-chain [:role/brewery :role/distributor
                  :role/big-market :role/small-market
                  :role/customer]
   :default-game-settings {:round-amount 20
                           :user-demands 5
                           :initial-stock 5
                           :stock-cost-factor 5
                           :debt-cost-factor 10}
   :user-role-image-path "img/roles"
   :property-descriptions #:round{:order "Wie viel hat diese Einheit bestellt (in Stück)?"
                                  :demand "Wie viel wurde von dieser Einheit bestellt (in Stück)?"
                                  :stock "Wie viel hat diese Einheit im Lager (in Stück)?"
                                  :cost "Wie viel Kosten hat diese Einheit (in Dollar)?"
                                  :debt "Wie viel Schulden hat diese Einheit bei der nächsten Einheit (in Stück)?"
                                  :incoming "Wie viel hat diese Einheit von der vorherigen Einheit bekommen (in Stück)?"
                                  :outgoing "Wie viel hat diese Einheit der nächsten Einheit geliefert?"
                                  :commited? "Hat diese Einheit bereits bestellt?"
                                  :ready? "Ist diese Einheit bereit für die nächste Runde?"}})

(def supply-chain
  "Define the order in which the roles form a supply chain."
  (:supply-chain definitions))

(def default-game-settings
  "The default game settings map."
  (:default-game-settings definitions))

(def websocket-endpoint
  "The relative url of the websocket endpoint."
  (:websocket-endpoint definitions))

(def websocket-packer
  "The packer used for websocket communication."
  (:websocket-packer definitions))

(def public-root
  (:public-root definitions))

(def game-logo
  "The main logo for the game."
  (:game-logo definitions))

(def game-area-path
  "The path where the images for the game-area backgrounds are stores."
  (:game-area-path definitions))

(def icon-path
  "The path were general icons are stored"
  (:icon-path definitions))

(def realms
  "The list of available realms."
  (definitions :realms))
(def player-realm
  "The key of the player realm."
  :realm/player)
(def leader-realm
  "The key of the leader realm."
  :realm/leader)
(def player-realm-data
  "The config-data associated with the player-realm."
  (-> player-realm :realms player-realm))
(def leader-realm-data
  "The config-data associated with the leader-realm."
  (-> leader-realm :realms leader-realm))

(def customer-role
  "The key for the customer role (which the server acts as)."
  :role/customer)
(def user-roles
  "A map of available user roles, from the role-keys to the associated data."
  (definitions :user-roles))
(def allowed-user-roles
  "A list of the user role keys."
  (-> definitions :user-roles keys set))
(def player-user-roles
  "A list of user role keys the players are allowed to act as."
  (disj allowed-user-roles customer-role))

(def user-role-image-path
  "The base path for where the images associated with the user roles are stored."
  (:user-role-image-path definitions))

(def game-title
  "The name of the game."
  (:game-title definitions))

(def property-descriptions
  "Descriptions for each property the data for a role can have (per round)."
  (:property-descriptions definitions))

(defn round-property->description
  "Takes a round-property key and returns its description."
  ([key] (round-property->description key "Keine Beschreibung vorhanden."))
  ([key default]
   (get property-descriptions key default)))

(defn user-role->image
  "Takes the name of a user role and returns a path
  to an image that represents it."
  [role-key]
  (if-let [img (get-in user-roles [role-key :icon])]
    (let [path (if (vector? img) (rand-nth img) img)]
      (str public-root "/" user-role-image-path "/" path ".svg"))
    ""))

(defn user-role->title
  "Returns the title of given user role."
  [user-role]
  (get-in user-roles [user-role :title]))

(defn user-role->color
  "Returns the hex color value associated with given user-role"
  [user-role]
  (get-in user-roles [user-role :color]))

#?(:clj
   ;; Server Side Configuration
   (do
     (def server-config
       "The config file for admins to edit.
  Should only be visible on the server."
       (read-string (slurp "server-config.edn")))

     (def leader-password
       "The password for the game leader.
  Only available on the server."
       (:leader-password server-config))))
