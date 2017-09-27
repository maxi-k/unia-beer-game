(ns beer-game.auth-test
  (:require [beer-game.auth :as auth]
            [beer-game.config :as config]
            [beer-game.store :as store]
            [clojure.test :refer :all]))

(deftest general-authentication
  (let [client-id "aldkfh-asdf-asdf--adf--adf"
        realm :player]
    (testing "Recognizes Invalid requests"
      (is (= [:auth/login-invalid {:realm "invalid-realm"}]
             (:message (auth/authenticate! client-id {:realm "invalid-realm"}))))
      (is (= [:auth/login-invalid {:key :invalid-key}]
             (:message (auth/authenticate! client-id {:realm realm
                                                      :key :invalid-key})))))))

(deftest login-player
  (let [client-id "asdfasldf-asdf-as-df-asdf-a-sdf"
        realm :player
        key :brewery]
    (testing "Can Authenticate as Brewery"
      (is (= [:auth/login-success {:uid key :client-id client-id :realm realm}]
             (:message (auth/authenticate! client-id {:realm realm :key key}))))
      #_(is (contains? (store/user-id->client-id key) client-id))
      #_(is (= key (auth/client-id->uid client-id))))))

(deftest login-leader
  (let [client-id "akfdhohiu-asdf-as-df-asfd-asfd"
        realm :leader
        uid :customer
        key config/leader-password]
    (testing "Can Authenticate as Leader"
      (is (= [:auth/login-success {:uid uid :client-id client-id :realm realm}]
             (:message (auth/authenticate! client-id {:realm realm :key key}))))
      #_(is (contains? (store/user-id->client-id uid) client-id))
      #_(is (= uid (auth/client-id->uid client-id))))))
