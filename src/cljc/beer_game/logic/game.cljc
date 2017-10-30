(ns beer-game.logic.game
  (:require [clojure.spec.alpha :as spec]
            [beer-game.spec.game :as game-spec]))

(defn handle-commit
  "Handles the game-round commit requested by a client."
  [cur-game-data commit]
  cur-game-data)


(spec/fdef handle-commit
           :args (spec/cat :cur-game-data :game/data
                           :commit :game/round-commit)
           :ret :game/data-update)
