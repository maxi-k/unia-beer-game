(ns beer-game.store
  (:require [beer-game.config :as config]
            [beer-game.server-utils :as util]))

(def #^{:private true} data-map
  "User model:"
  (ref {
        ;; Clients that have not logged in yet,
        ;; represented by their client-id
        :clients/unauthorized #{}
        ;; Logged-in Clients need the following information {
        ;; :realm      :: one of #{:player :leader}
        ;; :role       :: one of config/allowed-user-ids
        ;; :event      :: event-id
        ;; }
        ;; where :client-data points to a map from {server-uuid -> client-data}
        :clients/data {}
        ;; and :clients is a map from {client-id -> server-uuid}
        :clients/list {}
        }))

(if config/development?
  (add-watch data-map :log
             (fn [_ _ _ new] (println new))))

(defn add-client!
  "Adds given client to the client map.
  Returns it's server-uuid."
  [client-id]
  (let [uuid (util/uuid)]
    (dosync (alter data-map assoc-in [:clients/list client-id] uuid))
    uuid))

(defn remove-clients!
  "Removes the given client-ids from the client-id map."
  [client-ids]
  (dosync
   (alter data-map update :clients/list #(apply dissoc % client-ids))))

(defn request->user-id!
  "Takes a request and returns a user-id (uid).
  May alter the data-map."
  [{:as req :keys [client-id]}]
  (or
   (get-in @data-map [:clients/list client-id])
   (add-client! client-id)))

(def message->user-id! request->user-id!)

(defn client-id->user-id
  "Converts given client-id to a user-id"
  [client-id]
  (get-in @data-map [:clients/list client-id]))

(defn user-id->client-id
  "Converts given user-id to a set of client-ids."
  [user-id]
  (reduce
   (fn [coll [client-id server-uuid]]
     (if (= server-uuid user-id)
       (conj coll client-id)
       coll))
   #{} (:clients/list @data-map)))
