(ns beer-game.util-test
  (:require [beer-game.util :as util]
            [beer-game.config :as config]
            [beer-game.spec.game]
            [clojure.test :refer :all]
            [clojure.spec.alpha :as spec]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.test.alpha :as stest]))

(deftest filter-role-data
  (testing "The role-data filter-function only leaves the specified user-role data in the rounds vector."
    (let [test-data (gen/generate (spec/gen :game/rounds))
          user-role (rand-nth (keys config/user-roles))
          other-roles (-> config/user-roles keys set (disj user-role))
          filtered-data (util/filter-round-data test-data user-role)]
      (is (every? map? filtered-data))
      (is (every? map? (:game/roles filtered-data)))
      (is (every? (fn [round]
                    (every?
                     #(not (contains? other-roles %))
                     (keys (:game/roles round))))
                  filtered-data)))))
