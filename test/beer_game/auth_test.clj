(ns beer-game.auth-test
  (:require [beer-game.auth :as auth]
            [beer-game.config :as config]
            [clojure.test :refer :all]))

(deftest general-authentication
  (let [uuid "aldkfh-asdf-asdf--adf--adf"
        realm :player]
    (testing "Recognizes Invalid requests"
      (is (= [:auth/login-invalid {:realm "invalid-realm"}]
             (auth/authenticate! uuid {:realm "invalid-realm"})))
      (is (= [:auth/login-invalid {:key :invalid-key}]
             (auth/authenticate! uuid {:realm realm
                                       :key :invalid-key}))))))

(deftest login-player
  (let [uuid "asdfasldf-asdf-as-df-asdf-a-sdf"
        realm :player
        key :brewery]
    (testing "Can Authenticate as Brewery"
      (is (= [:auth/login-success {:uid key :uuid uuid :realm realm}]
             (auth/authenticate! uuid {:realm realm :key key})))
      (is (contains? (auth/uid->uuid key) uuid))
      (is (= key (auth/uuid->uid uuid))))))

(deftest login-leader
  (let [uuid "akfdhohiu-asdf-as-df-asfd-asfd"
        realm :leader
        uid :customer
        key config/leader-password]
    (testing "Can Authenticate as Leader"
      (is (= [:auth/login-success {:uid uid :uuid uuid :realm realm}]
             (auth/authenticate! uuid {:realm realm :key key})))
      (is (contains? (auth/uid->uuid uid) uuid))
      (is (= uid (auth/uuid->uid uuid))))))
