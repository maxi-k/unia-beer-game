(ns beer-game.spec.game
  (:require [clojure.spec.alpha :as s]
            [beer-game.config :as config]))

;; TODO: Stub
(s/def ::stock nat-int?)

(s/def ::role-data (s/keys :req-un [::stock]))
(s/def :user/role config/allowed-user-roles)
(s/def :game/roles
  (s/map-of :user/role ::role-data))

(s/def ::round-amount pos-int?)
(s/def ::demand nat-int?)
(s/def ::user-demands
  (s/or :constant ::demand
        :changing (s/coll-of ::demand)))
(s/def :game/settings
  (s/keys :req-un [::round-amount ::user-demands]
          :opt []))

(s/def :game/round
  (s/keys :req [:game/roles]))

#_(s/def ::game-round-setting
    (s/with-gen
      (fn [game-map] (= (count (:game/rounds game-map))
                       (get-in game-map [:game/settings :round-amount])))
      (gen/bind
       (s/gen :game/data)
       #(gen))))

(s/def ::game-round-bound
  (fn [game-map] (< (:game/current-round game-map)
                   (count (:game/rounds game-map)))))

(s/def :game/rounds (s/coll-of :game/round))
(s/def :game/current-round nat-int?)
(s/def :game/data
  (s/and
   (s/keys :req [:game/settings :game/rounds :game/current-round]
           :opt [])
   ::game-round-bound
   ;; Does not yet work because the generator can't find a conforming value
   ;; withing 100 tries.
   ;;::game-round-setting
   ))


(s/def :round/order nat-int?)
(s/def :game/round-commit
  (s/keys :req [:round/order :user/role]
          :opt []))

(s/def :game/data-update
  (s/keys :req []
          :opt []))
