(ns beer-game.logic.game
  (:require [clojure.spec.alpha :as spec]
            [beer-game.spec.game :as game-spec]
            [beer-game.config :as config]))

(defn handle-commit
  "Handles the game-round commit requested by a client."
  [cur-game-data commit]
  cur-game-data)

(spec/fdef handle-commit
           :args (spec/cat :cur-game-data :game/data
                           :commit :game/round-commit)
           :ret :game/data-update)

(defn init-round-role
  "Initializes the round role information for one user"
  [user-role]
  {:round/stock  0 :round/cost    0
   :round/demand 0 :round/request 0})

(defn init-round-roles
  "Initializes the role map for one round."
  []
  (reduce
   (fn [coll item]
     (assoc coll item (init-round-role item)))
   {}
   config/supply-chain))

(defn init-game-round
  "Initializes the map for one game round."
  []
  {:game/roles (init-round-roles)})

(defn init-game-rounds
  "Initializes the rounds-vector for a game."
  [round-amount]
  (vec
   (repeat (inc round-amount) (init-game-round))))

(defn init-game-data
  "Initializes a game-data map or ensures
  complete game-data for the given partial data-map."
  ([] (init-game-data {}))
  ([{:as data :keys [:game/settings]}]
   (cond-> data
     true (update :game/settings #(merge config/default-game-settings %))
     (nil? (:game/current-round data)) (assoc :game/current-round 0)
     (empty? (:game/rounds data)) (assoc :game/rounds
                                         (init-game-rounds
                                          (:round-amount settings))))))

(defn overall-cost
  "Takes a round vector and returns the overall cost for the entire game
  (summing up the cost for each round) for the given user-role."
  [rounds user-role]
  (reduce
   (fn [cost round]
     (+ cost (get-in round [:game/roles user-role :round/cost] 0)))
   0
   rounds))
