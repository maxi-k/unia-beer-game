(ns beer-game.test-util
  (:require [beer-game.config :as config]
            [beer-game.store :as store]
            [beer-game.handlers.auth :as auth]
            [beer-game.message-handler :as msg]
            [beer-game.server-utils :as sutil]
            [clojure.test :refer :all]
            [clojure.data :as data]))

(defn filter-msgs
  [result filter-fn]
  (if (vector? result)
    (filter #(filter-fn %) result)
    (and (filter-fn result) result)))

(defn authentication-msg
  [result]
  (-> result
      (filter-msgs #(and (= (:type %) :reply)
                         (= (namespace (nth (:message %) 0)) "auth")))
      first))

(defn as-leader!
  [function]
  (let [client-id (sutil/uuid)
        realm config/leader-realm
        key config/leader-password
        {:as message [msg data] :message}
        (authentication-msg (auth/authenticate! client-id {:user/realm realm :auth/key key}))]
    (function [msg data] message)))

(defn as-player!
  [login-data function]
  (let [client-data (merge {:client/id (str (sutil/uuid))
                            :user/realm config/player-realm}
                           login-data)
        {:as message [msg data] :message}
        (authentication-msg (auth/authenticate! (:client/id client-data)
                                                client-data))]
    (function [msg data] message)))

(def test-event-data
  {:event/id "TEST-EVENT"
   :event/name "Test event for automated testing."})

(defn with-test-event!
  ([event-data function]
   (let [event (store/create-event! event-data)]
     (function event)
     (store/destroy-event! (:event/id event-data))))
  ([function]
   (with-test-event! test-event-data function)))

;; Test Utility functions defined here
(deftest test-utility-functions
  (let [event-id (str (sutil/uuid))]
    (testing "Can test stuff within an event with with-test-event"
      (with-test-event!
        {:event/id event-id :event/name "Test event for automated testing."}
        (fn [event]
          (is (= true (:created event)))
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
