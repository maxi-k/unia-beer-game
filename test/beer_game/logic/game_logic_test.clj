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
  (let [game-datas (gen/sample (spec/gen :game/data))
        commit-datas (gen/sample (spec/gen :game/round-commit))]
    (doseq [game-data game-datas
            commit-data commit-datas
            :let [settings (:game/settings game-data)
                  {res :game/data :as update-map} (handle-commit game-data commit-data)
                  old-round (:game/current-round game-data)
                  new-round (:game/current-round res)]]
      (testing "Does not change game-settings."
        (is (= (:game/settings game-data)
               (:game/settings res))))
      (testing "The round update function produces the expected result."
        (if (not (:update/valid? update-map))
          ;; if the commit was not valid,
          ;; the rounds vector should be the same
          (do
            (println (:update/reason update-map))
            (is (= (:game/rounds game-data)
                   (:game/rounds res))))
          (let [supply-chain (get settings :game/supply-chain config/supply-chain)
                role-key (:user/role commit-data)
                old-data (get-in game-data [:game/rounds old-round :game/roles])
                new-data (get-in res [:game/rounds new-round :game/roles])
                [pre-role post-role] (roles-around role-key supply-chain)
                ;; Expected calculated according to mathematical formulas
                ;; -> see external documentation
                expected-incoming (min (get-in0 old-data [pre-role :round/stock])
                                       (+ (get-in0 old-data [pre-role :round/demand])
                                          (get-in0 old-data [pre-role :round/debt])))
                expected-stock (+ (get-in0 old-data [role-key :role/stock])
                                  expected-incoming)
                expected-debt (+ (get-in0 old-data [role-key :round/debt])
                                 (- (get-in0 old-data [role-key :round/demand])
                                    (get-in0 old-data [role-key :round/stock])))
                expected {:round/demand (get-in0 old-data [post-role :round/order])
                          :round/order (get commit-data :round/order 0)
                          :round/debt expected-debt
                          :round/stock expected-stock
                          :round/cost (+ (* (get settings :stock-cost-factor 0)
                                            expected-stock)
                                         (* (get settings :debt-cost-factor 0)
                                            expected-debt))}]
            (doseq [[k expected-value] expected]
              (testing (str "Round data spec for role " role-key " and value " k)
                (is (= (get-in0 new-data [role-key k])
                       expected-value))))))))))
