(ns beer-game.components.game-title
  (:require [beer-game.config :as config]
            [clojure.string :as string]))

(def game-title
  config/game-title)

(defn game-title-split
  "The game title split by words."
  ([as] (game-title-split as {}))
  ([as options]
   (let [split (-> config/game-title
                   (string/split #"\s")
                   (#(interpose [:br] %)))]
     (fn [as options]
       (-> (into [as options] split))))))
