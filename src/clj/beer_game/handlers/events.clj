(ns beer-game.handlers.events
  (:require [beer-game.store :as store]
            [beer-game.config :as config]))

(defn- with-auth
  [msg reply-fn]
  (let [{:as user-data :keys [:user/realm]} (store/message->user-data msg)]
    (if (or (nil? user-data)
            (not= realm config/leader-realm))
      {:type :reply
       :message [:auth/unauthorized {:user/realm realm}]}
      (if (fn? reply-fn)
        (reply-fn msg)
        reply-fn))))

(defmulti handle-event-msg
  "Dispatches all 'events' messages"
  :internal-id)

(defmethod handle-event-msg
  :fetch
  [ev-msg]
  (with-auth
    ev-msg
    (fn [msg]
      (let [events (get-in msg [:?data :event/id] :all)]
        (println events)
        {:type :reply
         :message [:event/list (store/events events)]}))))

(defmethod handle-event-msg
  :create
  [ev-msg]
  (with-auth
    ev-msg
    (fn [{:as msg :keys [?data]}]
      ;; TODO: Create new event here
      (let [safe-data (select-keys ?data [:event/id :event/name])
            result-data (store/create-event! safe-data)]
        {:type :broadcast
         :uids (store/user-data->client-id
                (fn [[user-id {:keys [:user/realm]}]]
                  (= realm config/leader-realm)))
         :message [:event/created result-data]}))))
