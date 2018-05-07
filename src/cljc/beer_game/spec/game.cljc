(ns beer-game.spec.game
  "Specifications for the data-structures represeting a game instance."
  #?(:clj (:require [clojure.spec.gen.alpha :as gen]
                    [clojure.spec.alpha :as spec]))
  (:require [clojure.spec.alpha :as s]
            [beer-game.config :as config]))

;; The stock one player has in a round
(s/def :round/stock nat-int?)
;; The items that are still due, aka the ones the player has
;; not yet been able to deliver
(s/def :round/debt nat-int?)
;; The cost a player has in one round
(s/def :round/cost nat-int?)
;; The demand a player has to fulfill for the player
;; one step lower in the supply chain
(s/def :round/demand (s/nilable nat-int?))
;; The items that were incoming for the given player
;; in a round
(s/def :round/incoming nat-int?)
;; The items that a given player can deliver
(s/def :round/outgoing nat-int?)
;; Indicates whether a piece of round-data has been
;; commited by the player
(s/def :round/commited? boolean?)
;; Indicates whether a player is ready for the next round
(s/def :round/ready? boolean?)
;; The order a player has for the player
;; up 1 step from the supply chain
(s/def :round/order nat-int?)

(s/def ::role-data
  (s/keys :opt [:round/stock :round/cost
                :round/demand :round/debt
                :round/incoming :round/outgoing
                :round/commited? :round/ready?]))
(s/def :user/role config/allowed-user-roles)
(s/def :game/roles
  (s/map-of :user/role ::role-data))


(s/def ::initial-stock nat-int?)
(s/def ::initial-incoming :round/incoming)
(s/def ::initial-outgoing :round/outgoing)

(s/def ::round-amount pos-int?)
(s/def ::demand nat-int?)

(s/def ::cost-factor pos-int?)
(s/def ::stock-cost-factor ::cost-factor)
(s/def ::debt-cost-factor  ::cost-factor)

(s/def ::delay pos-int?)
(s/def ::goods-delay ::delay)
(s/def ::communication-delay ::delay)

(s/def ::user-demands
  (s/or :constant ::demand
        :changing (s/coll-of ::demand
                             :min-count 1
                             :into [])))

(s/def :game/supply-chain
  (s/coll-of :user/role
             :distinct true
             :min-count 2
             :into []))

(s/def :game/settings
  (s/keys :req-un [::round-amount ::user-demands
                   ::stock-cost-factor ::debt-cost-factor
                   ::initial-stock ::initial-incoming ::initial-outgoing
                   ::goods-delay ::communication-delay]
          :req [:game/supply-chain]))

(s/def :game/round
  (s/keys :opt [:game/roles]))

(s/def ::game-round-bound
  #(or (zero? (:game/current-round %))
       (< (:game/current-round %)
          (count (:game/rounds %)))))

(s/def ::game-round-amount-bound
  (fn [data]
    (< (count (:game/rounds data))
       (inc (get-in data [:game/settings :round-amount])))))

(s/def ::game-rounds-supply-chain-bound
  (fn [data]
    (let [supply-chain (set (get-in data [:game/settings :game/supply-chain]
                                    config/supply-chain))]
      (every?
       (fn [round]
         (every?
          (fn [role]
            (contains? supply-chain role))
          (keys (get round :game/roles {}))))
       (:game/rounds data)))))

(s/def :game/rounds (s/coll-of :game/round))
(s/def :game/current-round nat-int?)
(s/def :game/data
  (s/and
   (s/keys :req [:game/settings :game/rounds :game/current-round])
   ::game-round-bound
   ::game-round-amount-bound
   ;; ::game-rounds-supply-chain-bound
   ))

(s/def :game/round-commit
  (s/keys :req [:round/order :user/role]
          :opt []))

(s/def :update/diff map?)
(s/def :update/valid? boolean?)
(s/def :update/reason map?)
(s/def :game/data-update
  (s/keys :req [:game/data :update/diff :update/valid?]
          :opt [:update/reason]))


#?(:clj

   (defn random-game-data
     "Creates random data required to create a game."
     []
     {:game/data
      {:game/settings (first (gen/sample (spec/gen :game/settings)))}})
   )
