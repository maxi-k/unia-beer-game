(ns beer-game.spec.game
  (:require [clojure.spec.alpha :as s]
            [beer-game.config :as config]))

;; TODO: Stub
;; The stock one player has in a round
(s/def :round/stock nat-int?)
;; The cost a player has in one round
(s/def :round/cost nat-int?)
;; The demand a player has to fulfill for the player
;; one step lower in the supply chain
(s/def :round/demand nat-int?)
;; The order a player has for the player
;; up 1 step from the supply chain
(s/def :round/order nat-int?)

(s/def ::role-data (s/keys :opt [:round/stock :round/cost
                                 :round/demand :round/order]))
(s/def :user/role config/allowed-user-roles)
(s/def :game/roles
  (s/map-of :user/role ::role-data))

(s/def ::round-amount pos-int?)
(s/def ::demand nat-int?)
(s/def ::initial-stock nat-int?)
(s/def ::user-demands
  (s/or :constant ::demand
        :changing (s/coll-of ::demand)))
(s/def :game/settings
  (s/keys :req-un [::round-amount ::user-demands ::initial-stock]
          :opt []))

(s/def :game/round
  (s/keys :req [:game/roles]))

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

(s/def :game/data-update
  (s/keys :req []
          :opt []))
