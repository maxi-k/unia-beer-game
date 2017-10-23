(ns beer-game.logic.spec
  (:require [clojure.spec.alpha :as s]
            [beer-game.config :as config]))

;; TODO: Stub
(s/def ::role-data map?)
(s/def ::round-data map?)

(s/def ::role-list
  (s/map-of config/allowed-user-roles
            ::role-data))

(s/def :game/rounds (s/coll-of ::round-data))
(s/def :game/roles ::role-list)
(s/def :game/data-map
  (s/keys :req [:game/roles :game/rounds]
          :opt []))

(s/def :round/order nat-int?)
(s/def :game/round-commit
  (s/keys :req [:round/order]
          :opt []))

(s/def :game/data-update
  (s/keys :req []
          :opt []))
