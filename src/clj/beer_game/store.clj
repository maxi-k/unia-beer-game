(ns beer-game.store
  (:require [beer-game.config :as config]
            [beer-game.server-utils :as util]))

(defn create-store
  []
  (ref {
        ;; Clients that have not logged in yet,
        ;; represented by their client-id
        :clients/unauthorized #{}
        ;; and :clients is a map from {client-id -> user-id}
        :clients/authorized {}
        ;; Users need the following information {
        ;; :realm      :: one of #{:player :leader}
        ;; :role       :: one of config/allowed-user-ids
        ;; :event      :: event-id
        ;; }
        ;; where :client-data points to a map from {user-id -> user-data}
        :users/data {}
        }))

(def #^{:private true} data-map
    "User model:"
  (create-store))

(if config/development?
  (add-watch data-map :log
             (fn [_ _ _ new] (println new))))

(defn add-client!
  "Adds given client to the unauthorized-clients set.
  Returns the client-id given."
  [client-id]
  (dosync (alter data-map update :clients/unauthorized conj client-id))
  client-id)

(defn remove-client!
  "Removes the given client-id."
  [client-id]
  (dosync
   (alter data-map update :clients/unauthorized disj client-id)
   (alter data-map update :clients/authorized dissoc client-id)))

(defn remove-clients!
  "Removes the given client-ids from the client-id map."
  [client-ids]
  (dosync
   (alter data-map update :clients/unauthorized #(apply disj % client-ids))
   (alter data-map update :clients/authorized #(apply dissoc % client-ids))))

(defn auth-client!
  "Authorizes the given client by removing him from the unauthorized list
  and adding him to the authorized map. If no uid was given, generates a new one."
  ([client-id user-id]
   (dosync
    (alter data-map update :clients/unauthorized disj client-id)
    (alter data-map update :clients/authorized assoc client-id user-id))
   user-id)
  ([client-id] (auth-client! client-id (util/uuid))))

(defn logout-client!
  "Logs out given client."
  [client-id]
  (dosync
   (alter data-map update :clients/authorized dissoc client-id)
   (alter data-map update :clients/unauthorized conj client-id)))

(defn client-id->user-id
  "Converts given client-id to a user-id"
  [client-id]
  (get-in @data-map [:clients/authorized client-id]))

(defn message->user-id!
  "Takes a message and returns a user-id (uid).
  May alter the data-map."
  [{:as msg :keys [client-id]}]
  (or
   (client-id->user-id client-id)
   (add-client! client-id)))

(defn user-id->client-id
  "Converts given user-id to a set of client-ids."
  [user-id]
  (reduce
   (fn [coll [client-id server-uuid]]
     (if (= server-uuid user-id)
       (conj coll client-id)
       coll))
   #{} (:clients/authorized @data-map)))

;; Return the data-map ref
data-map
