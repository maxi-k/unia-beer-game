(ns beer-game.handlers.auth
  (:require [beer-game.config :as config]
            [beer-game.store :as store]
            [beer-game.messages :as msgs]))


(def logged-in?
  "Returns nil if given client-id is not logged in."
  store/client-id->user-id)

(defn auth-leader
  "Tries to authenticate the given client-id with given password in the leader realm."
  [client-id client-key]
  (if (= client-key config/leader-password)
    (let [data {:user/role :role/customer
                :user/realm config/leader-realm
                :event/id :event/all
                :client/id client-id}
          user-id (store/auth-user! data)]
      [:auth/login-success (msgs/enrich-user user-id data)])
    [:auth/login-invalid {:auth/key client-key}]))

(defn auth-player
  "Tries to authenticate the given client-id in the player realm."
  [client-id event-id client-key]
  (let [event (store/events event-id)]
    (cond
      (or (not (contains? config/allowed-user-roles client-key))
          (contains? (get-in config/allowed-user-roles [client-key :except])
                     config/player-realm))
      [:auth/login-invalid {:auth/key client-key}]
      ;; ---------
      (nil? event)
      [:auth/login-invalid {:event/id event-id}]
      ;; ---------
      (and (:event/started? event)
           (not (contains?
                 (set (get-in event [:game/data :game/settings :game/supply-chain]))
                 client-key)))
      [:auth/login-invalid {:user/role client-key}]
      ;; ---------
      :else
      (let [data {:user/role client-key
                  :user/realm config/player-realm
                  :event/id event-id
                  :client/id client-id}
            user-id (store/auth-user! data)]
        [:auth/login-success (msgs/enrich-user user-id data)]))))

(defn authenticate!
  "Authenticate with given realm and key."
  [client-id {:keys [:user/realm :auth/key] event-id :event/id}]
  {:pre [(string? client-id)]}
  [{:type :reply
    :message
    (condp = realm
      config/player-realm (auth-player client-id event-id (keyword key))
      config/leader-realm (auth-leader client-id key)
      [:auth/login-invalid {:user/realm realm}])}
   {:type :broadcast
    :uids (store/leader-clients)
    :message (msgs/event-list :event/all)}
   {:type :broadcast
    :uids (store/event->clients event-id)
    :message (msgs/game-data event-id (store/client-id->user-id client-id))}])

(defn logout!
  "Logs out given client."
  [client-id]
  (store/logout-client! client-id)
  [{:type :reply
    :message (msgs/logout-success client-id)}
   {:type :broadcast
    :uids (store/leader-clients)
    :message (msgs/event-list :event/all)}])

(defn handle-msg
  "Dispatch method for handling authentication requests."
  [{:as ev-msg :keys [internal-id client-id ?data]}]
  (condp = internal-id
    :login (authenticate! client-id ?data)
    :logout (logout! client-id)
    {:type ::unhandled}))
