(ns beer-game.message-handler
  "Dynamic message handler that dispatches all incoming websocket messages
  to the respective handlers, and sends the result to the clients that are
  supposed to receive them.
  Also serves as an internal message dispatcher using a `clojure.core/async` csp
  channel [[msg-chan]] for communication between the domain-specific handlers."
  (:require [clojure.core.async :as async]
            [beer-game
             [config :as config]
             [store :as store]
             [util :as util]]
            [beer-game.handlers
             [events :as events-handler]
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

  ;; ASYNC INTERNAL MESSAGES
  (declare handle-internally-dispatched)
  (def msg-chan
    "The `core.async` csp channel where internal messages arrive.
  Continually listened to by [[receive-internal]]."
    (async/chan 20))

  (defn receive-internal
    "Handles internal messages my listening to [[msg-chan]]
    and passing on the messages to `handle-internally-dispatched`"
    []
    (async/go-loop []
      (handle-internally-dispatched (async/<! msg-chan))
      (recur)))
  (receive-internal)

  (defn dispatch-internal
    "Dispatches an internal message asynchronously by putting
    it on `msg-chan`.  "
    [orig-msg [msg data :as internal-msg]]
    ;; Piggypack the internal message with the other data
    ;; of the original (incoming) message
    (let [final-message (assoc orig-msg
                               :id msg
                               :?data data)]
      (println final-message)
      (async/put! msg-chan final-message)))

  ;;; UTILITY FUNCTIONS

  (defn broadcast
    "Sends given `message` to all connected uids.
    If a second argument is given, broadcasts it only to those listed in `uids`."
    ([message] (broadcast message (:any @connected-uids)))
    ([message uids]
     (if (fn? message)
       (doseq [uid uids] (send! uid (message uid)))
       (doseq [uid uids] (send! uid message)))))

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
     internal-msg]
    (if (vector? internal-msg)
      (doseq [msg internal-msg] (outbox! ev-msg msg))
      (let [{:keys [type message internal-id]} internal-msg]
        (if (and (nil? message)
                 (not= :noop type)
                 (some? :type))
          (println "Nil message from internal message: " internal-msg)
          (condp = type
            :internal (dispatch-internal ev-msg internal-msg)
            :reply (if (:?reply-fn ev-msg)
                     ((?reply-fn ev-msg) message)
                     (send! (or (:uid internal-msg) uid) message))
            :user (broadcast message (store/user-id->client-id (or (:uid internal-msg)
                                                                   internal-id)))
            :broadcast (broadcast message (or (:uids internal-msg)
                                              (:any @connected-uids)))
            :noop "Don't reply to anyone."
            (println "Unhandled internal message: " internal-id
                     " from incoming message: " (:id ev-msg)))))))

  (defn with-auth
    "Only executes send-fn with the message as argument if
    user is logged in. Sends an unauthorized message otherwide."
    [msg send-fn]
    (if (auth/logged-in? (:client-id msg))
      (send-fn msg)
      (send! (:client-id msg) [:auth/unauthorized {:auth/no-login :client-id}])))

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
    [ev-msg]
    (with-auth
      ev-msg
      (fn [msg]
        (outbox!
         msg
         (-> msg
             msg->inbox
             game-handler/handle-game-msg)))))

  (defmethod event :event
    [ev-msg]
    (with-auth
      ev-msg
      (fn [msg]
        (outbox! msg
                 (-> msg
                     msg->inbox
                     events-handler/handle-event-msg)))))

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

  (defn handle-internally-dispatched
    "Handles an internally dispatched message."
    [msg]
    (event msg))

  (defn event-handler
    [msg]
    {:pre [(contains? msg :id)]}
    (event msg))

  ;; Return a function that handles an incoming message
  ;; using the multimethod & functions defined above
  event-handler)
