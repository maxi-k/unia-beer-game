(ns beer-game.logic.game-logic-test
  (:require [beer-game.logic.game :refer :all]
            [beer-game.config :as config]
            [beer-game.store :as store]
            [beer-game.test-util :as test-util]
            [clojure.test :refer :all]
            [clojure.spec.alpha :as spec]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.test.alpha :as stest]))

(deftest commit-handler-test
  (testing "Conforms to spec."
    (stest/check `handle-commit))
  (let [game-data (gen/generate (spec/gen :game/data))
        commit-data (gen/generate (spec/gen :game/round-commit))
        {res :game/data} (handle-commit game-data commit-data)]
    (testing "Does not change game-settings."
      (is (= (:game/settings game-data)
             (:game/settings res))))))
