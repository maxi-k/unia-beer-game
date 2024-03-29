(ns beer-game.logic.game
  "The domain-specific game logic. Used to initialize, interpret and
  update the game data maps according to the beer-game algorithms.
  This namespace is available on both client and server, though it is currently
  only really used from the server side."
  (:require [clojure.spec.alpha :as spec]
            [beer-game.util :as util]
            [beer-game.spec.game :as game-spec]
            [beer-game.config :as config]))

(defn get-in0
  "Like `clojure.core/get-in` but with 0 for a not-found or `nil` value."
  [coll ks]
  (or (get-in coll ks 0) 0))

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

(defn calc-to-deliver
  "Given the round-data for one user-role, returns the amount
  of goods they have to deliver."
  [role-data]
  (+ (or (:round/demand role-data) 0)
     (or (:round/debt role-data) 0)))

(defn role-stock-update-fn
  "Update all the stocks after all players have commited to a round."
  {:deprecated true}
  [rounds cur-round]
  (fn [roles]
    (reduce
     (fn [coll [role role-data]]
       (let [round-data (get-in rounds [cur-round :game/roles role])
             cur-stock (:round/stock round-data)
             cur-debt (:round/debt round-data)]
         (assoc coll role
                (let [to-deliver (calc-to-deliver role-data)
                      ;; delivered (min to-deliver (or cur-stock 0))
                      incoming (or (:round/incoming role-data) 0)
                      diff (- incoming to-deliver)]
                  (-> role-data
                      (assoc :round/stock
                             (max 0 (+ cur-stock diff)))
                      (assoc :round/debt
                             (max 0 (+ cur-debt
                                       (- (- cur-stock to-deliver))))))))))
     {}
     roles)))

(defn update-stocks
  "Update all the stock values when a round is done."
  {:deprecated true}
  [rounds cur-round {:as settings :keys [:game/supply-chain]}]
  (let [next-round (inc cur-round)
        last-round? (>= next-round (count rounds))
        updater (role-stock-update-fn rounds cur-round)]
    (if last-round?
      rounds
      (update-in rounds [next-round :game/roles] updater))))

(defn calc-deliverable
  "Calculates how much a the given unit can deliver,
  and if possible, how much of its debt it can pay off."
  ([round-roles role]
   (min (get-in0 round-roles [role :round/stock])
        (+ (get-in0 round-roles [role :round/demand])
           (get-in0 round-roles [role :round/debt]))))
  ([role-data]
   (min (get role-data :round/stock 0)
        (+ (get role-data :round/demand 0)
           (get role-data :round/debt 0)))))


(defn apply-round-update
  "Applies a commit to the game rounds vector."
  [rounds cur-round {:as settings :keys [:game/supply-chain
                                         stock-cost-factor debt-cost-factor
                                         communication-delay goods-delay]}
   {:as commit :keys [:round/order :user/role]}]
  (let [[pre post] (roles-around role supply-chain)
        next-round (inc cur-round)
        round-count (count rounds)
        goods-round (+ cur-round goods-delay)
        communication-round (+ cur-round communication-delay)
        old-data (get-in rounds [cur-round :game/roles])
        new-order (or order 0)
        ;; --- first role in supply chain
        ;; --- just gets its order fulfilled
        outgoing (calc-deliverable old-data role)
        other-incoming outgoing
        other-demand new-order
        new-stock (+ (get-in0 old-data [role :round/stock])
                     (- (get-in0 old-data [role :round/incoming])
                        outgoing))
        new-debt (max 0
                      (+ (get-in0 old-data [role :round/debt])
                         (- (get-in0 old-data [role :round/demand]) outgoing)))
        new-cost (+ (* stock-cost-factor new-stock)
                    (* debt-cost-factor new-debt))]
    (cond-> rounds
      true (assoc-in [cur-round :game/roles role :round/commited?] true)
      true (assoc-in [cur-round :game/roles role :round/outgoing] outgoing)

      (and (some? pre)
           (< communication-round round-count))
      (assoc-in [communication-round :game/roles pre :round/demand] other-demand)

      (and (some? post)
           (< goods-round round-count))
      (assoc-in [goods-round :game/roles post :round/incoming] other-incoming)

      ;; we are the brewery
      (and (nil? pre)
           (< goods-round round-count))
      (assoc-in [goods-round :game/roles role :round/incoming] order)

      (not (>= next-round round-count))
      (update-in [next-round :game/roles role] merge
                 #:round{:debt new-debt
                         :cost new-cost
                         :stock new-stock}))))

(defn apply-user-ready
  "Applies an update to the given round that signals
  that the given user is ready for the next round."
  [round user-role]
  (assoc-in round [:game/roles user-role :round/ready?] true))

(defn round-completed?
  "Takes the current round map and the game supply chain
  and returns true if the round is completed."
  [{:as round-data :keys [:game/roles]}
   supply-chain]
  (let [relevant-roles (butlast supply-chain)]
    (every? #(and (get-in roles [% :round/commited?])
                  (get-in roles [% :round/ready?]))
            relevant-roles)))

(defn calc-user-demand
  "Returns the user-demand for the `current-round` given the
  `settings` of the game."
  [settings current-round]
  (if-let [demands (:user-demands settings)]
    (cond
      (not (sequential? demands)) demands
      (>= current-round (count demands)) (last demands)
      :else (nth demands current-round (:user-demands config/default-game-settings)))
    (calc-user-demand current-round
                      (assoc settings :user-demands
                             (:user-demands config/default-game-settings)))))

(defn maybe-next-round
  "Takes an update data map and applies the transformations
  required to start the next round. Also requires the current-round
  and the game settings to be passed.
  If the round is not completed yet, just returns the update-data map. "
  [{:as update-data
    {:as game-data
     cur-round :game/current-round
     {:as settings
      :keys [:game/supply-chain]} :game/settings} :game/data}]
  (if (round-completed?
       (get-in game-data [:game/rounds cur-round])
       supply-chain)
    (-> update-data
        (update-in [:game/data :game/rounds]
                   apply-round-update
                   cur-round settings
                   {:user/role :role/customer
                    :round/order (calc-user-demand settings cur-round)})
        ;; Update the stock of every role at the end
        #_(update-in [:game/data :game/rounds]
                     update-stocks cur-round settings)
        (update-in [:game/data :game/current-round] inc)
        (#(assoc-in % [:update/diff :game/current-round]
                    (get-in % [:game/data :game/current-round]))))
    update-data))

(defn handle-commit
  "Handles the game-round commit requested by a client.
  Endpoint for calls from the outside."
  [{:as cur-game-data
    cur-round :game/current-round
    cur-rounds :game/rounds
    settings :game/settings}
   {:as commit
    commit-orders :round/order
    user-role :user/role}]
  (let [update-map {:game/data cur-game-data
                    :update/diff {}
                    :update/valid? true}
        supply-chain (set (get settings :game/supply-chain config/supply-chain))]
    (cond
      (not (contains? supply-chain user-role))
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
      (let [{:as settings :keys [:game/supply-chain]}
            (get-in update-map [:game/data :game/settings])]
        ;; Apply the round update
        (-> update-map
            (update-in [:game/data :game/rounds]
                       apply-round-update cur-round settings commit)
            ;; Add it to the diff
            (#(assoc-in % [:update/diff :game/rounds]
                        (get-in % [:game/data :game/rounds]))))))))

(spec/fdef handle-commit
           :args (spec/cat :cur-game-data :game/data
                           :commit :game/round-commit)
           :ret :game/data-update)

(defn handle-ready
  "Handles a clients signal that they are ready for the next round."
  [{:as cur-game-data
    cur-round :game/current-round
    cur-rounds :game/rounds
    settings :game/settings}
   {:as ready-data
    target-round :target-round
    user-role :user/role}]
  (let [update-map {:game/data cur-game-data
                    :update/diff {}
                    :update/valid? true}
        supply-chain (set (get settings :game/supply-chain config/supply-chain))]
    (cond
      (not (contains? supply-chain user-role))
      (assoc update-map
             :update/valid? false
             :update/reason {:game/supply-chain supply-chain
                             :user/role user-role})
      ;; -----
      (>= cur-round (count cur-rounds))
      (assoc update-map
             :update/valid? false
             :update/reason {:game/current-round cur-round
                             :game/rounds (count cur-rounds)})
      ;; -----
      (get-in cur-rounds [cur-round :game/roles user-role :round/ready?])
      (assoc update-map
             :update/valid? false
             :update/reason {:round/ready? true})
      ;; -----
      (not (get-in cur-rounds [cur-round :game/roles user-role :round/commited?]))
      (assoc update-map
             :update/valid? false
             :update/reason {:round/commited? false})
      ;; -----
      :else
      (-> update-map
          (update-in [:game/data :game/rounds cur-round]
                     apply-user-ready user-role)
          ;; Go to the next round if everyone is ready
          maybe-next-round))))

(defn start-game
  "Updates the given `game-data` map by adding all the data
  required to start the game."
  [game-data user-roles]
  (let [all-roles (conj user-roles (last config/supply-chain))
        supply-chain (filter #(contains? all-roles %) config/supply-chain)]
    (-> game-data
        (assoc-in [:game/settings :game/supply-chain] supply-chain))))

(defn init-round-role
  "Initializes the round role information for one user."
  [{:as settings :keys [initial-stock
                        initial-incoming
                        initial-outgoing]} user-role]
  (let [data {:round/stock initial-stock
              :round/debt 0
              :round/cost  0
              :round/incoming initial-incoming
              :round/outgoing initial-outgoing
              :round/demand initial-outgoing
              :round/order initial-incoming}]
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
   (or (:game/supply-chain settings) config/supply-chain)))

(defn init-game-round
  "Initializes the map for one game round."
  [settings]
  {:game/roles (init-round-roles settings)})

(defn init-game-rounds
  "Initializes the rounds-vector for a game."
  [{:as settings :keys [round-amount]}]
  (let [game-round (init-game-round settings)]
    (-> (repeat round-amount game-round)
        vec)))

(defn transform-game-settings
  "Transforms the game settings, trying to make it adhere to the
  spec (like transforming strings to integers)."

  [settings]
  (let [to-int #?(:clj #(if (nil? %) 0 (Integer. %))
                  :cljs #(if (nil? %) 0 js/parseInt))
        transform-map {:game/supply-chain identity
                       :user-demands #(cond (string? %) (to-int %)
                                            (sequential? %) (vec %)
                                            :else %)}]
    (util/apply-transformations settings transform-map to-int)))

(defn init-game-data
  "Initializes a game-data map or ensures
  complete game-data for the given partial data-map."
  ([] (init-game-data {}))
  ([{:as data :keys [:game/settings]}]
   (cond-> data
     true (update :game/settings transform-game-settings)
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
