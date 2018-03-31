(ns beer-game.components.game-title
  "Components for displaying the title of the game."
  (:require [beer-game.config :as config]
            [clojure.string :as string]))

(def game-title
  "The game-title as defined in the [[beer-game.config]] namespace."
  config/game-title)

(defn game-title-split
  "Component displaying the game title split by words."
  ([as] (game-title-split as {}))
  ([as options]
   (let [split (-> config/game-title
                   (string/split #"\s")
                   (#(interpose [:br] %)))]
     (fn [as options]
       (-> (into [as options] split))))))
