(ns beer-game.logic.game
  (:require [clojure.spec.alpha :as spec]
            [beer-game.logic.spec :as game-spec]))


(defn handle-commit
  "Handles the game-round commit requested by a client."
  [cur-game-data commit]
  {})

(spec/fdef handle-commit
           :args (spec/cat :cur-game-data :game/data-map
                           :commit :game/round-commit)
           :ret :game/data-update)
