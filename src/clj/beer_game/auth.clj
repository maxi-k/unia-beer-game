(ns beer-game.auth
  (:require [beer-game.config :as config]
            [beer-game.store :as store :refer [client-map]]))

(defn client-id->realm
  "Returns given realm"
  [client-id]
  (let [uid (get-in @client-map [:clients client-id])]
    (condp = (name uid)
      :customer :leader
      nil :unauthorized
      :player)))

(defn client-id->uid
  "Converts given client-id to a user-id"
  [client-id]
  (get-in @client-map [:clients client-id]))

(def logged-in?
  "Returns nil if given client-id is not logged in."
  client-id->uid)

(defn auth-leader
  "Tries to authenticate the given client-id with given password in the leader realm."
  [client-id key]
  (if (= key config/leader-password)
    (do
      (store/add-client! client-id)
      [:auth/login-success {:uid :customer :client-id client-id
                            :realm config/leader-realm}])
    [:auth/login-invalid {:key key}]))

(defn auth-player
  "Tries to authenticate the given client-id in the player realm."
  [client-id key]
  (if (contains? config/allowed-user-ids key)
    (do
      (store/add-client! client-id)
      [:auth/login-success {:uid key :client-id client-id
                            :realm config/player-realm}])
    [:auth/login-invalid {:key key}]))

(defn authenticate!
  "Authenticate with given realm and key."
  [client-id {:keys [realm key]}]
  {:pre (string? client-id)}
  {:type :reply
   :message
   (condp = realm
     config/player-realm (auth-player client-id (keyword key))
     config/leader-realm (auth-leader client-id key)
     [:auth/login-invalid {:realm realm}])})


(defn logout!
  "Logs out given client."
  [client-id]
  (store/remove-clients! [client-id])
  {:type :reply
   :message [:auth/logout-success {:client-id client-id}]})
