(ns beer-game.store
  (:require [beer-game.config :as config]
            [beer-game.server-utils :as util]))

(defn create-store
  []
  (ref {
        ;; :clients/authorized is a map from {client-id -> user-id}
        :clients/authorized {}
        ;; Users need the following information {
        ;; :user/realm      :: one of #{:player :leader}
        ;; :user/role       :: one of config/allowed-user-ids
        ;; :event/id        :: event-id
        ;; }
        ;; where :user/data points to a map from {user-id -> user-data}
        :user/data {}
        ;; A map from {event-id -> event-data}
        ;; where event-data must contains
        ;; :event/id        :: id of the event (same as map key)
        ;; :event/name      :: human readable event name
        :event/data {}
        ;; A map from {event-id -> game-data}
        :game/data {}
        }))

(def data-map
  "User model:"
  (create-store))

#_(if config/development?
    (add-watch data-map :log
               (fn [_ _ _ new] (println new))))

(defn authorized-clients
  "Returns the current authorized-clients-map from the store."
  []
  (:clients/authorized @data-map))

(defn client-id->user-id
  "Converts given client-id to a user-id"
  [client-id]
  (get-in @data-map [:clients/authorized client-id]))

(def message->user-id
  "Takes a message and returns a user-id."
  (comp
   client-id->user-id
   :client-id))

(defn user-id->user-data
  "Takes a user-id and returns the user-data stored."
  [user-id]
  (get-in @data-map [:user/data user-id]))

(def message->user-data
  "Takes a message and returns the user-data stored."
  (comp
   user-id->user-data
   message->user-id))

(def client-id->user-data
  (comp
   user-id->user-data
   client-id->user-id))

(defn user-id->client-id
  "Converts given user-id to a set of client-ids."
  [user-id]
  {:pre (string? user-id)
   :post (set? %)}
  (reduce
   (fn [coll [client-id server-uuid]]
     (if (= server-uuid user-id)
       (conj coll client-id)
       coll))
   #{} (:clients/authorized @data-map)))

(defn filter-user-data
  "Filters the user data using given function `f`,
  returning a map of only those entries for which `f` returned true.
  f is given a vector of [key value] as argument."
  [f]
  (transduce (filter f) conj
             {} (:user/data @data-map)))

(defn event-id-user-role-filter
  [event-id user-role]
  (fn [[user-id data]]
    (and (= event-id  (:event/id  data))
         (= user-role (:user/role data)))))

(defn user-data-fn->client-id
  "Given a filter-function on user data, returns all matching client-ids."
  [filter-fn]
  (->> filter-fn
       filter-user-data
       keys
       (mapcat user-id->client-id)
       set))

(defn user-data->client-id
  "Takes some user-data with at least :event/id and :user/role
  and tries to find all associated client ids. Returns a set."
  [user-data]
  (user-data-fn->client-id
   (event-id-user-role-filter (:event/id user-data)
                              (:user/role user-data))))

(defn user-data->user-id
  "Takes some user-data with at least :event/id and :user/role
  and tries to find the associated user id.
  Returns a vector [user-id user-data]."
  [{:keys [:user/role]
    event-id :event/id}]
  (first
   (filter-user-data
    (event-id-user-role-filter event-id role))))

(defn leader-clients
  "Returns all client-ids which are associated with leader-users."
  []
  (user-data-fn->client-id
   (fn [[user-id {:keys [:user/realm]}]]
     (= realm config/leader-realm))))

(defn remove-client!
  "Removes the given client-id."
  [client-id]
  (dosync
   (alter data-map update :clients/authorized dissoc client-id)))

(defn remove-clients!
  "Removes the given client-ids from the client-id map."
  [client-ids]
  (dosync
   (alter data-map update :clients/authorized #(apply dissoc % client-ids))))

(defn auth-client!
  "Authorizes the given client by adding him to the authorized map.
  If no uid was given or it is nil, generates a new one."
  ([client-id user-id]
   (let [user-id (or user-id (util/uuid))]
     (dosync
      (alter data-map update :clients/authorized assoc client-id user-id))
     user-id))
  ([client-id] (auth-client! client-id nil)))

(defn user-data!
  "Adds or modifies the user-data for given user-id to the map.
  If `user-data` is a function, applies it to the existing data
  and saves the result."
  [user-id user-data]
  (dosync
   (if (fn? user-data)
     (alter data-map update-in [:user/data user-id] user-data)
     (alter data-map assoc-in [:user/data user-id] user-data))))

(def logout-client!
  "Logs out given client."
  remove-client!)

(defn logout-user!
  "Logs out all clients that are associated with given `user-id`.
  Returns the list of clients logged out."
  [user-id]
  {:pre (string? user-id)
   :post (set? %)}
  (dosync
   (let [clients (user-id->client-id user-id)]
     (doseq [client clients] (logout-client! client))
     clients)))

(defn auth-user!
  "Authorizes given user by adding the client-id<->user-id
  relation to the authorized map and adding given user-data
  to the data-map. Does not check for permissions or validity.
  Returns the associated user-id."
  [{:as data
    :keys [:user/realm :user/role]
    event-id :event/id
    client-id :client/id}]
  (let [;; Use given user-id or try to find it using user-data
        [user-id _] (or (:user/id data) (user-data->user-id data))
        ;; Add given or new user-id to authorized ids
        user-id (auth-client! client-id user-id)]
    (user-data! user-id {:user/realm    realm
                         :user/role     role
                         :event/id event-id})
    user-id))

(defn single-event?
  "Returns true if given event-id refers to a single event."
  [id]
  (not (or (= :event/all id)
           (= :all id))))

(defn events
  "Returns a list of events stored."
  ([] (get @data-map :event/data))
  ([id]
   (if (single-event? id)
     (get-in @data-map [:event/data id])
     (events))))

(defn event->users
  "Returns a map from user-id -> user-data where the user is part of given event-id"
  [event-id]
  (filter-user-data
   (fn [[user-id user-data]]
     (= (:event/id user-data) event-id))))

(defn create-event!
  "Creates a new event with given id and given data."
  [{:as event-data :keys [:event/id]}]
  (if (or (nil? id)
          (contains? (events) id))
    {:created false
     :reason :event/id
     :event/id id}
    (do
      (dosync
       (alter data-map assoc-in [:event/data id] event-data))
      (assoc event-data :created true))))

(defn destroy-event!
  "Destroys given event and logs out all the users associated with it.
  Returns a map with the key :clients for the list of clients that were logged out,
  and the key :message with relevant data concerning the deletion."
  [event-id]
  (if (nil? event-id)
    {:clients #{}
     :message {:destroyed false
               :reason :event/id
               :event/id event-id}}
    (let [users (keys (event->users event-id))
          clients (ref nil)]
      (dosync
       (alter data-map update :event/data dissoc event-id)
       (ref-set clients (set (mapcat logout-user! users))))
      {:clients @clients
       :message {:destroyed true
                 :event/id event-id}})))
