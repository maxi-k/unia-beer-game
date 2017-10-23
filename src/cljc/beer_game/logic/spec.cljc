(ns beer-game.logic.spec
  (:require [clojure.spec.alpha :as s]
            [beer-game.config :as config]))

;; TODO: Stub
(s/def ::stock nat-int?)
(s/def ::role-data
  (s/keys :req-un [::stock]))

(s/def :user/role config/allowed-user-roles)

(s/def :game/roles
  (s/map-of :user/role ::role-data))

(s/def :game/round
  (s/keys :req [:game/roles]))

(s/def :game/rounds (s/coll-of :game/round))
(s/def :game/current-round nat-int?)
(s/def :game/data-map
  (s/keys :req [:game/rounds :game/current-round]
          :opt []))

(s/def :game/settings
  (s/keys :req []
          :opt []))

(s/def :round/order nat-int?)
(s/def :game/round-commit
  (s/keys :req [:round/order :user/role]
          :opt []))

(s/def :game/data-update
  (s/keys :req []
          :opt []))
