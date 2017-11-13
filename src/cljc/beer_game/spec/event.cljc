(ns beer-game.spec.event
  #?(:clj (:require [clojure.spec.gen.alpha :as gen]))
  (:require [clojure.spec.alpha :as s]
            [beer-game.spec.game]))

(s/def :event/id (s/and string? (partial re-matches #"^\S+$")))
(s/def :event/name (s/and string? (partial re-matches #"^(\S+\s*\S*)+$")))
(s/def :event/data
  (s/keys :req [:game/data :event/id :event/name]))

#?(:clj
   (defn random-event-data
     "Creates some random event data"
     []
     (-> (s/gen :event/data)
         gen/sample
         last
         (update :game/data select-keys [:game/settings]))))
