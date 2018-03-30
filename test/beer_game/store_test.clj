(ns beer-game.store-test
  "Tests for the accessors and update-functions for the
  [[beer-game.store]] store of the server"
  (:require [beer-game.store :as store]
            [beer-game.test-util :as test-util]
            [clojure.test :refer :all]))

(deftest store-accessors
  (test-util/test-all-roles!
   (fn [[msg {:as data
             client-id :client/id
             event-id :event/id}] _]
     (let [user-id (store/client-id->user-id client-id)]
       (testing "Client ID is stored after login"
         (is (contains? (store/authorized-clients) client-id)))
       (testing "Can convert from client-id to user-id and user-data"
         (is (some? user-id))
         (is (string? user-id))
         (is (map? (store/client-id->user-data client-id)))
         (is (= event-id (:event/id (store/client-id->user-data client-id)))))
       (testing "Can convert from user-id to client-id"
         (is (contains? (store/user-id->client-id user-id) client-id)))
       (testing "Can convert from user-data to user-id and client-ids"
         (is (= user-id (nth (store/user-data->user-id data) 0)))
         (is (contains? (store/user-data->client-id data) client-id)))
       (testing "Can list players of given event."
         (is (contains? (store/event->users event-id) user-id))))))
  (test-util/as-leader!
   (fn [[msg {client-id :client/id}] _]
     (testing "Leaders can be listed with `leader-clients`"
       (is (contains? (store/leader-clients) client-id))))))
