(ns beer-game.components.messages
  (:require [reagent.core :as ra]
            [soda-ash.core :as sa]
            [beer-game.util :as util]))

(defn no-login-system-msg
  [cause]
  {:message/icon "exclamation triangle"
   :message/title "Du wurdest abgemeldet."
   :message/content [:p "Grund dafür kann ein Neustart des Servers oder Datenverlust des Clients sein."
                     [:br] "Technische Informationen:"
                     [:br] cause]})

(defn no-permission-system-msg
  [content]
  {:message/icon "exclamation triangle"
   :message/title "Keine Berechtigung."
   :message/content [:p "Aktion konnte aufgrund mangelnder Berechtigungen nicht ausgeführt werden."
                     [:br]
                     content]
   :message/time 5000})

(defn no-permission-msg
  "A message telling the user that he does not have permission for something."
  ([reason] (no-permission-msg reason {}))
  ([reason options]
   [sa/Message (merge {:icon "exclamation triangle"
                       :heading "Keine Berechtigung."
                       :content reason
                       :warning true}
                      options)]))
