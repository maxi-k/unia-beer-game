(ns beer-game.messages
  (:require [beer-game.store :as store]
            [beer-game.util :as util]))

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

(defn events-status-only
  "Takes a map of events and returns a map of events without the game data."
  [events]
  (reduce
   (fn [coll [id val]]
     (assoc coll id (-> val (dissoc :game/data))))
   {}
   events))

(defn enrich-user
  "Enrich the user model with more information to be given back to the client."
  [user-id data]
  (let [{event-id :event/id :as store-data} (store/user-id->user-data user-id)
        event-data (if (util/single-event? event-id)
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
  (let [event-map (store/events events)
        event-map (if (util/single-event? events)
                    {events event-map}
                    event-map)]
    [:event/list (reduce (fn [coll [id data]]
                           (assoc coll id (enrich-event data)))
                         {}
                         event-map)]))


(defn game-data
  "A message for sending the relevant game data to the given user."
  [event-id user-id]
  (let [event (store/events event-id)
        event-users (store/event->users event-id)
        user-data (get event-users user-id)]
    (if (nil? user-data)
      nil
      (let [role (:user/role user-data)
            game-data (:game/data event)
            relevant-data
            (update game-data :game/rounds #(util/filter-round-data % role))]
        [:game/data relevant-data]))))
