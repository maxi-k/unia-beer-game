(ns beer-game.components.messages
  (:require [reagent.core :as ra]
            [soda-ash.core :as sa]))

(defn no-permission-msg
  "A message telling the user that he does not have permission for something."
  ([reason] (no-permission-msg reason {}))
  ([reason options]
   [sa/Message (merge {:icon "exclamation triangle"
                       :heading "Keine Berechtigung."
                       :content reason
                       :warning true}
                      options)]))
