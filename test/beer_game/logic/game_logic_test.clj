(ns beer-game.logic.game-logic-test
  "Tests the game logic in [[beer-game.logic.game]]
  using the specs in [[beer-game.spec.game]]."
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
  (let [game-datas (gen/sample (spec/gen :game/data) 5)
        commit-datas (gen/sample (spec/gen :game/round-commit) 5)]
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
          (is (= (:game/rounds game-data)
                 (:game/rounds res)))
          (let [{:keys [:goods-delay :communication-delay :game/supply-chain :round-amount]
                 :or {goods-delay (:goods-delay config/default-game-settings)
                      communication-delay (:communication-delay config/default-game-settings)
                      supply-chain config/supply-chain
                      round-amount (:round-amount config/default-game-settings)}} settings
                role-key (:user/role commit-data)
                old-data (get-in game-data [:game/rounds old-round :game/roles])
                new-rounds (get-in res [:game/rounds])
                new-data (get-in new-rounds [(inc old-round) :game/roles])
                [pre-role post-role] (roles-around role-key supply-chain)
                ;; Expected calculated according to mathematical formulas
                ;; -> see external documentation
                expected-outgoing (calc-deliverable old-data role-key)
                incoming-round (+ old-round goods-delay)
                demand-round (+ old-round communication-delay)
                expected-delivered (calc-deliverable old-data pre-role)
                ;; Expected incoming which is set in the 'post' role data at incoming round
                expected-incoming (cond (nil? post-role) nil
                                        (>= incoming-round round-amount) nil
                                        :else expected-outgoing)
                expected-stock (+ (get-in0 old-data [role-key :round/stock])
                                  (- (get-in0 old-data [role-key :round/incoming])
                                     expected-outgoing))
                expected-debt (max 0
                                   (+ (get-in0 old-data [role-key :round/debt])
                                      (- (get-in0 old-data [role-key :round/demand]) expected-outgoing)))
                expected-order (get commit-data :round/order 0)
                expected-demand (cond (nil? pre-role) nil
                                      (>= demand-round round-amount) nil
                                      :else expected-order)
                expected #:round{:debt expected-debt
                                 :stock expected-stock
                                 :cost (+ (* (get settings :stock-cost-factor 0)
                                             expected-stock)
                                          (* (get settings :debt-cost-factor 0)
                                             expected-debt))}]
            (testing (str "Incoming for other supply-chain member is set properly" post-role)
              (println expected-incoming incoming-round post-role (get-in new-rounds [incoming-round]))
              (is (= expected-incoming
                     (get-in new-rounds [incoming-round post-role :round/incoming]))))
            (if (some? pre-role)
              (testing (str "Demand for other supply-chain member is set properly " pre-role)
                (is (= expected-demand
                       (get-in new-rounds [demand-round pre-role :round/demand]))))
              (testing (str "Production for first supply-chain member is is set properly")
                (is (= expected-order
                       (get-in new-rounds [incoming-round role-key :round/incoming])))))
            (when (< (inc old-round) (count (get game-data :game/rounds)))
              (testing (str "Round data spec for role " role-key)
                (is (=
                     (select-keys (get new-data role-key)
                                  (keys expected))
                     expected)))
              #_(when (some? pre-role)
                  (testing (str "Round data spec for supplier " pre-role)
                    (is (= (get-in new-data [pre-role :round/order] 0)
                           expected-order)))))))))))
