(ns beer-game.messages
  (:require [beer-game.store :as store]))

(def public-event-keys
  "The keys in an event map that everyone should
  be able to see."
  [:event/id
   :event/name])

(defn enrich-event
  "Enrich given event-data for the client."
  [data]
  (let [users (-> data :event/id store/event->users vals)]
    (assoc data :user/list users)))

(defn enrich-user
  "Enrich the user model with more information to be given back to the client."
  [user-id data]
  (let [{event-id :event/id :as store-data} (store/user-id->user-data user-id)
        event-data (if (store/single-event? event-id)
                     (select-keys (store/events event-id)
                                  public-event-keys)
                     nil)]
    (-> data
        (merge store-data)
        (assoc :event/data event-data))))

(defn logout-forced
  [client-id]
  [:auth/logout-forced {:client/id client-id}])

(defn logout-success
  [client-id]
  [:auth/logout-success {:client/id client-id}])

(defn event-list
  [events]
  [:event/list (reduce (fn [coll [id data]]
                         (assoc coll id (enrich-event data)))
                       {}
                       (store/events events))])
