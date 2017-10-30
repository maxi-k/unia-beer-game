(ns beer-game.components.messages
  (:require [reagent.core :as ra]
            [soda-ash.core :as sa]
            [beer-game.config :as config]
            [beer-game.util :as util]))

(defn debug-msg
  "A message for debugging purposes only."
  [& message]
  {:message/icon "code"
   :message/title "Debugging"
   :message/content [:p (for [msg message] [:p {:key (str msg)} msg
                                            [:br]])]})

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

(defn logout-forced-msg
  [data]
  {:message/icon "question circle"
   :message/title "Du wurdest vom Server ausgeloggt."
   :message/content [:p "Grund dafür ist wahrscheinlich, dass der Spielleiter das Event beendet hat." [:br]
                     (if config/development? data)]
   :message/time 10000})

(defn event-not-destroyed-msg
  "A message for 'event could not be deleted' response from server."
  [reason]
  {:message/icon "exclamation triangle"
   :message/title "Event konnte nicht gelöscht werden."
   :message/content [:p "Das gewählte Event konnte nicht gelöscht werden. Technische Informationen:"
                     [:br]
                     reason]})

(defn invalid-submission-msg
  "A message for invalid form submissions."
  []
  #:message {:icon "exclamation triangle"
             :title "Bitte eine vollständige und gültige Eingabe tätigen."
             :time 3000})
