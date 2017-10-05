(ns beer-game.handlers.auth
  (:require [beer-game.config :as config]
            [beer-game.store :as store]))


(def logged-in?
  "Returns nil if given client-id is not logged in."
  store/client-id->user-id)

(defn enrich-user
  "Enrich the user model with more information to be given back to the client."
  [user-id data]
  (let [{event-id :event/id :as store-data} (store/user-id->user-data user-id)
        event-data (if (store/single-event? event-id)
                     (store/events event-id)
                     nil)]
    (-> data
        (merge store-data)
        (assoc :event/data event-data))))

(defn auth-leader
  "Tries to authenticate the given client-id with given password in the leader realm."
  [client-id key]
  (if (= key config/leader-password)
    (let [data {:user/role :role/customer
                :user/realm config/leader-realm
                :event/id :event/all
                :client/id client-id}
          user-id (store/auth-user! data)]
      [:auth/login-success (enrich-user user-id data)])
    [:auth/login-invalid {:auth/key key}]))

(defn auth-player
  "Tries to authenticate the given client-id in the player realm."
  [client-id event-id key]
  (if (contains? config/allowed-user-roles key)
    (if (store/events event-id)
        (let [data {:user/role key
                    :user/realm config/player-realm
                    :event/id event-id
                    :client/id client-id}
              user-id (store/auth-user! data)]
          [:auth/login-success (enrich-user user-id data)])
      [:auth/login-invalid {:event/id event-id}])
    [:auth/login-invalid {:auth/key key}]))

(defn authenticate!
  "Authenticate with given realm and key."
  [client-id {:keys [:user/realm :auth/key] event-id :event/id}]
  {:pre (string? client-id)}
  {:type :reply
   :message
   (condp = realm
     config/player-realm (auth-player client-id event-id (keyword key))
     config/leader-realm (auth-leader client-id key)
     [:auth/login-invalid {:user/realm realm}])})

(defn logout!
  "Logs out given client."
  [client-id]
  (store/remove-client! client-id)
  {:type :reply
   :message [:auth/logout-success {:client/id client-id}]})

(defn handle-msg
  "Dispatch method for handling authentication requests."
  [{:as ev-msg :keys [internal-id client-id ?data]}]
  (condp = internal-id
    :login (authenticate! client-id ?data)
    :logout (logout! client-id)
    {:type ::unhandled}))
