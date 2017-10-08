(ns beer-game.handlers.events-test
  (:require [beer-game.test-util :as test-util]
            [beer-game.handlers.events :as events]
            [beer-game.config :as config]
            [beer-game.store :as store]
            [clojure.test :refer :all]
            [clojure.data :as data]))

(deftest event-creation
  (testing "Events without event-id will not be created."
    (test-util/with-test-event!
      {:event/id nil :event/name "Null event for testing" }
      (fn [event]
        (is (= false (:created event)))
        (is (= (:reason event) :event/id))
        (is (nil? (:event/id event))))))
  (testing "Events with same event-id can't be created twice."
    (let [event-data {:event/id "TEST-EVENT" :event/name "Null event for testing" }]
      (test-util/with-test-event!
        event-data
        (fn [event1]
          (test-util/with-test-event!
            event-data
            (fn [event2]
              (is (= true (:created event1)))
              (is (= false (:created event2)))
              (is (= (:reason event2) :event/id))
              (is (= (:event/id event2)
                     (:event/id event1))))))))))

(deftest event-destruction
  (let [client-id "my-cool-client-id"]
    (test-util/with-test-event!
      (fn [{event-id :event/id}]
        (test-util/as-player!
         {:auth/key :role/brewery
          :client/id client-id
          :event/id event-id}
         (fn [[msg data] _]
           (testing "Login for event destruction test was successful"
             (is (contains? (store/authorized-clients) client-id))
             (is (= event-id (:event/id (store/client-id->user-data client-id)))))
           ;; We are now logged in with a player into given event
           (let [{:keys [clients message] :as res} (store/destroy-event! event-id)]
             (println res)
             (testing "Event was destroyed."
               (is (:destroyed message))
               (is (not (contains? (store/events) event-id))))
             (testing "Players in given event are logged out."
               (is (not (contains? (store/authorized-clients) client-id))))
             (testing "Log-out function returns a list of logged out clients."
               (println clients)
               (is (contains? clients client-id))))))))))
