(ns beer-game.test-util
  "Contains various utilities for writing tests."
  (:require [beer-game.config :as config]
            [beer-game.store :as store]
            [beer-game.handlers.auth :as auth]
            [beer-game.message-handler :as msg]
            [beer-game.server-utils :as sutil]
            [clojure.spec.alpha :as spec]
            [clojure.spec.gen.alpha :as gen]
            [beer-game.spec.event :as event-spec]
            [beer-game.spec.game :as game-spec]
            [clojure.test :refer :all]
            [clojure.data :as data]
            [beer-game.spec.game :as game-spec]))

(defn filter-msgs
  "Filters the messages from the `result` of a message handler
  using the given `filter-fn`. The passed `result` may or may not
  represent multiple messages - both cases are handled."
  [result filter-fn]
  (if (vector? result)
    (filter filter-fn result)
    (and (filter-fn result) result)))

(defn authentication-msg
  "Takes the result from an authentication requests and filters out
  the one message that contains the authentication message."
  [result]
  (-> result
      (filter-msgs #(and (= (:type %) :reply)
                         (= (namespace (nth (:message %) 0)) "auth")))
      first))

(defn as-leader!
  "Tests the given test `function` in the context of a game-leader.
  Uses the credentials stored on the server."
  [function]
  (let [client-id (sutil/uuid)
        realm config/leader-realm
        key config/leader-password
        {:as message [msg data] :message}
        (authentication-msg (auth/authenticate! client-id {:user/realm realm :auth/key key}))]
    (function [msg data] message)))

(defn as-player!
  "Tests the given test `function` in the context of a player
  using the provided `login-data`."
  [login-data function]
  (let [client-data (merge {:client/id (str (sutil/uuid))
                            :user/realm config/player-realm}
                           login-data)
        {:as message [msg data] :message}
        (authentication-msg (auth/authenticate! (:client/id client-data)
                                                client-data))]
    (function [msg data] message)))

(defn with-test-event!
  "Applies the given test function in the context of an event
  (creates it in the store). If no `event-data` is given, generates
  some using the specs in [[beer-game.spec.game]]."
  ([event-data function]
   (let [event-data (if (contains? event-data :game/data)
                      event-data
                      (assoc event-data :game/data (game-spec/random-game-data)))
         event (store/create-event! event-data)]
     (function event)
     (store/destroy-event! (:event/id event-data))))
  ([function]
   (with-test-event! (event-spec/random-event-data)
     function)))

(defn test-all-roles!
  "Calls the given testing function in the context of
  all possible roles (leader and player)."
  [testing-fn]
  (as-leader! testing-fn)
  (with-test-event!
    (fn [{event-id :event/id}]
      (as-player!
       {:event/id event-id :auth/key :role/brewery}
       testing-fn))))

;; Test Utility functions themselves shall be tested
(deftest test-utility-functions
  (let [event-id (str (sutil/uuid))]
    (testing "Can test stuff within an event with with-test-event"
      (with-test-event!
        {:event/id event-id :event/name "Test event for automated testing."}
        (fn [event]
          (is (true? (:created event)))
          (is (= event-id (:event/id event))))))
    (testing "Event used for testing with with-test-event is destroyed afterwards"
      (is (not (contains? (store/events) event-id)))))
  (testing "Can test stuff as leader with as-leader!"
    (as-leader!
     (fn [[msg msg-data] whole-mssage]
       (is (= msg :auth/login-success))
       (is (= (:user/realm msg-data) config/leader-realm)))))
  (testing "Can test stuff as player with as-player!"
    (with-test-event!
      (fn [{:as event event-id :event/id}]
        (as-player!
         {:event/id event-id
          :user/role :role/brewery
          :auth/key :role/brewery}
         (fn [[msg msg-data] whole-message]
           (is (= msg :auth/login-success))
           (is (= (:user/realm msg-data) config/player-realm))
           (is (= (:event/id msg-data) event-id))))))))
