(ns beer-game.handler-test
  (:require [beer-game.handler :refer :all]
            [beer-game.auth :as auth]
            [clojure.test :refer :all]))

(deftest starting-socket
  (testing "Starting the socket defines some vars"
    (is (some? channel-socket))
    (is (instance? clojure.lang.IDeref connected-uids))
    (is (some? event))
    (is (some? routes))))
