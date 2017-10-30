(ns beer-game.spec.event
  (:require [clojure.spec.alpha :as s]
            [beer-game.spec.game]))

(s/def :event/id (partial re-matches #"^\S+$"))
(s/def :event/name (partial re-matches #"^(\S+\s*\S*)+$"))
(s/def :event/data
  (s/keys :req [:game/data :event/id :event/name]))
