(ns beer-game.handlers.auth-test
  (:require [beer-game.handlers.auth :as auth]
            [beer-game.config :as config]
            [beer-game.store :as store]
            [beer-game.test-util :as test-util]
            [clojure.test :refer :all]
            [clojure.data :as data]))



(deftest general-authentication
  (let [client-id "aldkfh-asdf-asdf--adf--adf"
        realm :realm/player]
    (testing "Recognizes Invalid requests"
      (is (= [:auth/login-invalid {:user/realm "invalid-realm"}]
             (:message (test-util/authentication-msg
                        (auth/authenticate! client-id {:user/realm "invalid-realm"})))))
      (is (= [:auth/login-invalid {:auth/key :invalid-key}]
             (:message (test-util/authentication-msg
                        (auth/authenticate! client-id {:user/realm realm
                                                       :auth/key :invalid-key}))))))))

(deftest login-player
  (let [client-id "my-cool-client-id"]
    (test-util/with-test-event!
      (fn [{event-id :event/id}]
        (test-util/as-player!
         {:auth/key :role/brewery
          :client/id client-id
          :event/id event-id}
         (fn [[msg data] _]
           (let [required-data {:user/role :role/brewery
                                :client/id client-id
                                :user/realm config/player-realm}
                 [only-data only-required-data shared] (data/diff data required-data)]
             (testing "Can Authenticate as Brewery"
               (is (= :auth/login-success msg)))
             (testing "Result from Player authentication returns required data"
               (is (empty? only-required-data)))
             (testing "Store knows of user-id after authentication."
               (is (contains? (store/user-data->client-id data)
                              client-id))))))))))

(deftest login-leader
  (test-util/as-leader!
   (fn [[msg data] _]
     (let [req-data {:user/role :role/customer
                     :user/realm config/leader-realm}
           [only-data only-req-data shared] (data/diff data req-data)]
       (testing "Can authenticate as leader"
         (is (= :auth/login-success msg)))
       (testing "Result from Leader authentication includes required data"
         (is (empty? only-req-data)))
       (testing "Store knows of user-id after authentication"
         (is (contains? (store/user-data->client-id data)
                        (:client/id data))))))))
