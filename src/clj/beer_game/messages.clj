(ns beer-game.messages
  (:require [beer-game.store :as store]))

(defn enrich-event
  "Enrich given event-data for the client."
  [data]
  (let [users (-> data :event/id store/event->users vals)]
    (-> data
        (assoc :user/list users))))

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

(defn event-list
  [events]
  [:event/list (reduce (fn [coll [id data]]
                         (assoc coll id (enrich-event data)))
                       {}
                       (store/events events))])
