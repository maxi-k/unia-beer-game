(ns beer-game.handlers.events
  (:require [beer-game.store :as store]
            [beer-game.config :as config]
            [beer-game.messages :as msgs]))

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
        {:type :reply
         :message (msgs/event-list events)}))))

(defmethod handle-event-msg
  :create
  [ev-msg]
  (with-auth
    ev-msg
    (fn [{:as msg :keys [?data]}]
      ;; TODO: Create new event here
      (let [safe-data (select-keys ?data [:event/id :event/name :game/data])
            result-data (store/create-event! safe-data)]
        {:type :broadcast
         :uids (store/leader-clients)
         :message [:event/created (msgs/enrich-event result-data)]}))))

(defmethod handle-event-msg
  :destroy
  [ev-msg]
  (with-auth
    ev-msg
    (fn [{:as msg :keys [?data]}]
      (let [{:keys [clients message]} (store/destroy-event! (:event/id ?data))]
        (if (:destroyed message)
          [{:type :broadcast
            :uids (store/leader-clients)
            :message [:event/destroyed message]}
           {:type :broadcast
            :uids clients
            :message #(msgs/logout-forced %)}]
          {:type :reply
           :message [:event/destroyed message]})))))
