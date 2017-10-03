(ns beer-game.message-handler
  (:require [clojure.data :refer [diff]]
            ;; Server Adapter for websockets
            [beer-game
             [config :as config]
             [store :as store]
             [util :as util]]
            [beer-game.handlers
             [auth :as auth]
             [game :as game-handler]
             [system :as system-handler]]))


(defn message-handler
  "Wraps the websocket message handlers in a closure
  that takes the websocket component."
  [{:as websocket-component
    :keys [ring-ajax-post
           ring-ajax-get-or-ws-handshake
           incoming
           send!
           connected-uids]}]

  ;;; UTILITY FUNCTIONS

  (defn broadcast
    "Sends given `message` to all connected uids.
    If a second argument is given, broadcasts it only to those listed in `uids`."
    ([message] (broadcast message (:any @connected-uids)))
    ([message uids] (doseq [uid uids] (send! uid message))))

  (defn msg->inbox
    "Converts an incoming channel message to an 'inbox-message',
    which can then be dispatched to the internal handlers."
    [ev-msg]
    (merge ev-msg {:internal-id (-> ev-msg :id util/name-only)}))

  (defn outbox!
    "Takes the original (incoming) message and the outgoing message
  of the format {:type #{reply broadcast noop} :message message-vector}
  and sends it "
    [{:keys [uid ?reply-fn] :as ev-msg}
     {:keys [type message internal-id] :as internal-msg}]
    (condp = type
      :reply (send! (or (:uid internal-msg) uid) message)
      :reply-fn ((?reply-fn ev-msg) message)
      :user (broadcast message (store/user-id->client-id (or (:uid internal-msg)
                                                             internal-id)))
      :broadcast (broadcast message (or (:uids internal-msg)
                                        (:any @connected-uids)))
      :noop "Don't reply to anyone."
      (println "Unhandled internal message: " internal-id
               " from incoming message: " (:id ev-msg))))


  ;;; MESSAGE HANDLING

  (defmulti event
    "Dispatch on the id of the event message being sent to the socket,
    by splitting the namespaced id into a vector and dispatching on that."
    (fn [msg]
      (-> msg :id namespace keyword)))

  ;; Handles all authentication messages
  (defmethod event :auth
    [ev-msg]
    (outbox! ev-msg
             (-> ev-msg
                 msg->inbox
                 auth/handle-msg)))

  ;; Handles all game messages
  ;; Secure channel (user has to be logged in)
  (defmethod event :game
    [{:keys [client-id] :as ev-msg}]
    (if (auth/logged-in? client-id)
      (outbox!
       ev-msg
       (-> ev-msg
           msg->inbox
           game-handler/handle-msg))
      (send! client-id [:auth/unauthorized])))

  ;; Handles all system messages (chsk)
  (defmethod event :chsk
    [ev-msg]
    (outbox! ev-msg (-> ev-msg
                        msg->inbox
                        system-handler/handle-msg)))

  (defmethod event :testing
    [{:as ev-msg :keys [event ?data uid]}]
    (send! uid [:testing/echo ?data]))

  (defmethod event :default
    [{:keys [event]}]
    (println "Defaulted event: " event))

  ;; Return a function that handles an incoming message
  ;; using the multimethod & functions defined above
  (fn [msg]
    (event msg))

  )
