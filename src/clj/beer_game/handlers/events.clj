(ns beer-game.handlers.events
  "The message handler that deals with event-level things, like event creation,
  deletion, event-data fetching, and starting the associated game.
  Used by [[beer-game.message-handler]]."
  (:require [beer-game.store :as store]
            [beer-game.config :as config]
            [beer-game.messages :as msgs]))

(defn- with-auth
  "Utility function for assuring an endpoint (message) is only
  avaiable to someone authenticated as the leader account."
  ([msg reply-fn] (with-auth msg nil reply-fn))
  ([msg event-id reply-fn]
   (let [{:as user-data :keys [:user/realm :event/id]}
         (store/message->user-data msg)]
     (if (or (nil? user-data)
             (not (or (= realm config/leader-realm)
                      (= id event-id))))
       {:type :reply
        :message [:auth/unauthorized {:user/realm realm}]}
       (if (fn? reply-fn)
         (reply-fn msg user-data)
         reply-fn)))))


(defmulti handle-event-msg
  "Dispatches all 'events' messages."
  :internal-id)

(defmethod handle-event-msg
  :fetch
  [ev-msg]
  (with-auth
    ev-msg
    ;; Also allow users which participate in event to fetch
    (get-in ev-msg [:?data :event/id])
    (fn [msg user-data]
      (let [events (get-in msg [:?data :event/id] :all)
            event-msg (msgs/event-list events)]
        (if (= (:user/realm user-data) config/leader-realm)
          {:type :reply
           :message event-msg}
          {:type :reply
           :message (update event-msg 1 msgs/events-status-only)})))))

(defmethod handle-event-msg
  :create
  [ev-msg]
  (with-auth
    ev-msg
    (fn [{:as msg :keys [?data]} _]
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
    (fn [{:as msg :keys [?data]} _]
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

(defmethod handle-event-msg
  :start
  [{:as msg :keys [?data]}]
  (with-auth msg
    (fn [_ user-data]
      {:type :broadcast
       :uids (store/event->clients (:event/id ?data))
       :message
       [:event/started
        (->> ?data
             :event/id
             store/start-event!)]})))
