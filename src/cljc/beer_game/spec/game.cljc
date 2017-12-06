(ns beer-game.spec.game
  #?(:clj (:require [clojure.spec.gen.alpha :as gen]
                    [clojure.spec.alpha :as spec]))
  (:require [clojure.spec.alpha :as s]
            [beer-game.config :as config]))

;; TODO: Stub
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
                :round/demand :round/incoming
                :round/debt
                :round/commited? :round/ready?]))
(s/def :user/role config/allowed-user-roles)
(s/def :game/roles
  (s/map-of :user/role ::role-data))

(s/def ::round-amount pos-int?)
(s/def ::demand nat-int?)
(s/def ::initial-stock nat-int?)
(s/def ::cost-factor pos-int?)
(s/def ::user-demands
  ::demand
  #_(s/or :constant ::demand
          :changing (s/coll-of ::demand)))
(s/def :game/supply-chain
  (s/coll-of :user/role))
(s/def :game/settings
  (s/keys :req-un [::round-amount ::user-demands ::initial-stock ::cost-factor]
          :opt [:game/supply-chain]))

(s/def :game/round
  (s/keys :opt [:game/roles]))

(s/def ::game-round-bound
  (s/or :round-zero #(zero? (:game/current-round %))
        :round-in-bounds #(< (:game/current-round %)
                             (count (:game/rounds %)))))

(s/def :game/rounds (s/coll-of :game/round))
(s/def :game/current-round nat-int?)
(s/def :game/data
  (s/and
   (s/keys :req [:game/settings :game/rounds :game/current-round])
   ::game-round-bound
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