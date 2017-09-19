(ns beer-game.auth
  (:require [beer-game.config :as config]))

;; A map from client-id to user-id
(defonce uuid-map (ref {}))

(defn uuid->realm [uuid]
  (let [uid (get @uuid-map uuid)]
    (condp = (name uid)
      :customer :leader
      nil :unauthorized
      :player)))

(defn uuid->uid [uuid]
  (get @uuid-map uuid))

(defn uid->uuid [uid]
  (reduce
   (fn [coll [k vs]]
     (if (= vs uid)
       (conj coll k)
       coll))
   #{} @uuid-map))

(defn user-id-fn [req]
  (or
   (uuid->uid (:client-id req))
   (:client-id req)))

(defn auth-leader [uuid key]
  (if (= key config/leader-password)
    (do
      (dosync (alter uuid-map assoc uuid :customer))
      [:auth/login-success {:uid :customer :uuid uuid
                            :realm config/leader-realm}])
    [:auth/login-invalid {:key key}]))

(defn auth-player [uuid key]
  (if (contains? config/allowed-user-ids key)
    (do
      (dosync (alter uuid-map assoc uuid key))
      [:auth/login-success {:uid key :uuid uuid
                            :realm config/player-realm}])
    [:auth/login-invalid {:key key}]))

(defn authenticate!
  "Authenticate with given realm and key."
  [uuid {:keys [realm key]}]
  {:pre (string? uuid)}
  (condp = realm
    config/player-realm (auth-player uuid (keyword key))
    config/leader-realm (auth-leader uuid key)
    [:auth/login-invalid {:realm realm}]))

(defn logout!
  "Logs out given client."
  [uuid]
  (dosync (alter uuid-map dissoc uuid))
  [:auth/logout-success {:uuid uuid}])
