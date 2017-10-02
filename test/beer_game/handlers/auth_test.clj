(ns beer-game.handlers.auth-test
  (:require [beer-game
             [handlers.auth :as auth]
             [config :as config]
             [store :as store]]
            [clojure.test :refer :all]
            [clojure.data :as data]))

(deftest general-authentication
  (let [client-id "aldkfh-asdf-asdf--adf--adf"
        realm :realm/player]
    (testing "Recognizes Invalid requests"
      (is (= [:auth/login-invalid {:user/realm "invalid-realm"}]
             (:message (auth/authenticate! client-id {:user/realm "invalid-realm"}))))
      (is (= [:auth/login-invalid {:auth/key :invalid-key}]
             (:message (auth/authenticate! client-id {:user/realm realm
                                                      :auth/key :invalid-key})))))))

(deftest login-player
  (let [client-id "asdfasldf-asdf-as-df-asdf-a-sdf"
        realm :realm/player
        key :role/brewery]
    (testing "Can Authenticate as Brewery"
      (let [[msg data] (:message (auth/authenticate! client-id {:user/realm realm :auth/key key}))
            req-data {:user/role key
                      :client/id client-id
                      :user/realm realm}
            [only-data only-req-data shared] (data/diff data req-data)]

        (is (= :auth/login-success msg))
        (is (empty? only-req-data)) ;; result-data contains at least required-data
        #_(is (contains? (store/user-id->client-id key) client-id))
        #_(is (= key (auth/client-id->uid client-id)))))))

(deftest login-leader
  (let [client-id "akfdhohiu-asdf-as-df-asfd-asfd"
        realm :realm/leader
        key config/leader-password]
    (testing "Can Authenticate as Leader"
      (let [[msg data] (:message (auth/authenticate! client-id {:user/realm realm :auth/key key}))
            req-data {:user/role :role/customer
                      :client/id client-id
                      :user/realm realm}
            [only-data only-req-data shared] (data/diff data req-data)]

        (is (= :auth/login-success msg))
        (is (empty? only-req-data)) ;; result-data contains at least required-data
        #_(is (contains? (store/user-id->client-id uid) client-id))
        #_(is (= uid (auth/client-id->uid client-id)))))))
