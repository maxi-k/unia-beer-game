(ns beer-game.logic.game
  (:require [clojure.spec.alpha :as spec]
            [beer-game.spec.game :as game-spec]
            [beer-game.config :as config]))

(defn roles-around
  "Returns the roles before and after the passed role in the supply chain."
  [role supply-chain]
  (let [role-idx (.indexOf supply-chain role)
        chain-length (count supply-chain)]
    (if (< role-idx 0)
      [nil nil]
      (cond-> [nil nil]
        (< (inc role-idx) chain-length) (assoc 1 (nth supply-chain (inc role-idx)))
        (>= (dec role-idx) 0)           (assoc 0 (nth supply-chain (dec role-idx)))))))

(defn update-stocks
  "Update all the stock values when a round is done."
  [rounds cur-round {:as settings :keys [:game/supply-chain]}]
  (let [next-round (inc cur-round)
        last-round? (>= next-round (count rounds))
        updater (fn [roles]
                  (reduce
                   (fn [coll [role val]]
                     (let [cur-stock (get-in rounds [cur-round :game/roles role :round/stock])]
                       (assoc coll role
                              (assoc val :round/stock
                                     (max 0
                                          (+ cur-stock
                                             (- (or (:round/incoming val) 0)
                                                (or (:round/demand val) 0))))))))
                   {}
                   roles))]
    (if last-round?
      rounds
      (update-in rounds [next-round :game/roles] updater))))

(defn apply-round-update
  "Applies a commit to the game rounds vector."
  [rounds cur-round {:as settings :keys [:game/supply-chain cost-factor]}
   {:as commit :keys [:round/order :user/role]}]
  (let [[pre post] (roles-around role supply-chain)
        next-round (inc cur-round)
        last-round? (>= next-round (count rounds))
        role-data (get-in rounds [cur-round :game/roles role])
        post-data (get-in rounds [cur-round :game/roles post])
        cost (* cost-factor (or (:round/stock role-data) 0))
        delivered (min (or (:round/demand role-data) 0)
                       (or (:round/stock role-data) 0))]
    (cond-> rounds
      true (assoc-in [cur-round :game/roles role :round/commited?] true)
      true (assoc-in [cur-round :game/roles role :round/cost] cost)
      ;; --- add the delivered to the next in the supply chain
      ;; -- in the next round
      (and (some? post)
           (not last-round?))
      (assoc-in [next-round :game/roles post :round/incoming] delivered)
      ;; --- first role in supply chain
      (and (nil? pre)
           (not last-round?))
      (assoc-in [next-round :game/roles role :round/incoming] order)
      ;; --- if not first role in the supply chain
      ;; -- place the order from the commit as demand on the previous element
      (and (some? pre)
           (not last-round?))
      ;; place the own order
      (assoc-in [next-round :game/roles pre :round/demand] order))))

(defn round-completed?
  "Takes the current round map and the game supply chain
  and returns true if the round is completed."
  [{:as round-data :keys [:game/roles]}
   supply-chain]
  (let [relevant-roles (butlast supply-chain)]
    (every? #(get-in roles [% :round/commited?])
            relevant-roles)))

(defn handle-commit
  "Handles the game-round commit requested by a client."
  [{:as cur-game-data
    cur-round :game/current-round
    cur-rounds :game/rounds
    settings :game/settings}
   {:as commit
    commit-orders :round/order
    user-role :user/role}]
  (let [update-map {:game/data cur-game-data
                    :update/diff {}
                    :update/valid? true}]
    (cond
      (not (contains? (set (:game/supply-chain settings)) user-role))
      (assoc update-map
             :update/valid? false
             :update/reason {:game/supply-chain (:game/supply-chain settings)
                             :user/role user-role})
      ;; -----
      (>= cur-round (count cur-rounds))
      (assoc update-map
             :update/valid? false
             :update/reason {:game/current-round cur-round
                             :game/rounds (count cur-rounds)})
      ;; -----
      (get-in cur-rounds [cur-round :game/roles user-role :round/commited?])
      (assoc update-map
             :update/valid? false
             :update/reason {:round/commited? true})
      ;; -----
      :else
      (let [data-atom (atom update-map)
            {:as settings :keys [:game/supply-chain]}
            (get-in @data-atom [:game/data :game/settings])]
        ;; Apply the round update
        (swap! data-atom
               update-in [:game/data :game/rounds]
               apply-round-update cur-round settings commit)
        ;; Add it to the diff
        (swap! data-atom
               #(assoc-in % [:update/diff :game/rounds]
                          (get-in % [:game/data :game/rounds])))
        ;; Increase the current round if every round was commited
        (when (round-completed?
               (get-in @data-atom [:game/data :game/rounds cur-round])
               supply-chain)
          ;; Apply the user order
          (swap! data-atom
                 update-in [:game/data :game/rounds]
                 apply-round-update cur-round settings {:user/role :role/customer
                                                        :round/order (:user-demands settings)})
          ;; Update the stock of every role at the end
          (swap! data-atom update-in [:game/data :game/rounds]
                 update-stocks cur-round settings)

          (swap! data-atom update-in [:game/data :game/current-round] inc)
          (swap! data-atom #(assoc-in % [:update/diff :game/current-round]
                                      (get-in % [:game/data :game/current-round]))))
        @data-atom))))

(spec/fdef handle-commit
           :args (spec/cat :cur-game-data :game/data
                           :commit :game/round-commit)
           :ret :game/data-update)

(defn start-game
  [game-data user-roles]
  (let [all-roles (conj user-roles (last config/supply-chain))
        supply-chain (filter #(contains? all-roles %) config/supply-chain)]
    (-> game-data
        (assoc-in [:game/settings :game/supply-chain] supply-chain))))

(defn init-round-role
  "Initializes the round role information for one user."
  [{:as settings :keys [initial-stock]} user-role]
  (let [data {:round/stock initial-stock
              :round/debt 0
              :round/cost  0
              :round/demand 0
              :round/order 0}]
    (if (= (last config/supply-chain) user-role)
      (assoc data :round/demand (:user-demand settings))
      data)))

(defn init-round-roles
  "Initializes the role map for one round."
  [settings]
  (reduce
   (fn [coll item]
     (assoc coll item (init-round-role settings item)))
   {}
   config/supply-chain))

(defn init-game-round
  "Initializes the map for one game round."
  [settings]
  {:game/roles (init-round-roles settings)})

(defn init-game-rounds
  "Initializes the rounds-vector for a game."
  [{:as settings :keys [round-amount]}]
  (-> (repeat round-amount {})
      vec
      (assoc 0 (init-game-round settings))))

(defn init-game-data
  "Initializes a game-data map or ensures
  complete game-data for the given partial data-map."
  ([] (init-game-data {}))
  ([{:as data :keys [:game/settings]}]
   (cond-> data
     true (update :game/settings #(merge config/default-game-settings %))
     (nil? (:game/current-round data)) (assoc :game/current-round 0)
     (empty? (:game/rounds data)) (#(assoc % :game/rounds
                                            (init-game-rounds (:game/settings %)))))))

(defn overall-cost
  "Takes a round vector and returns the overall cost for the entire game
  (summing up the cost for each round) for the given user-role."
  [rounds user-role]
  (reduce
   (fn [cost round]
     (+ cost (get-in round [:game/roles user-role :round/cost] 0)))
   0
   rounds))
