(ns beer-game.components.messages
  (:require [clojure.string :as string]
            [reagent.core :as ra]
            [soda-ash.core :as sa]
            [beer-game.config :as config]
            [beer-game.util :as util]))

(defn render-message
  [{:as msg
    :keys [:message/title :message/content :message/icon]}]
  [sa/Message {:header title
               :content (cond
                          (nil? content) nil
                          (vector? content) (ra/as-element content)
                          :else (str content))
               :icon icon}])

(defn debug-msg
  "A message for debugging purposes only."
  [& message]
  #:message{:icon "code"
            :title "Debugging"
            :content [:p (for [msg message]
                           [:p {:key (str msg)} msg
                            [:br]])]})

(defn no-login-system-msg
  [cause]
  #:message{:icon "exclamation triangle"
            :title "Du wurdest abgemeldet."
            :content [:p "Grund dafür kann ein Neustart des Servers oder Datenverlust des Clients sein."
                      [:br] "Technische Informationen:"
                      [:br] cause]})

(defn no-permission-system-msg
  [content]
  #:message{:icon "exclamation triangle"
            :title "Keine Berechtigung."
            :content [:p "Aktion konnte aufgrund mangelnder Berechtigungen nicht ausgeführt werden."
                      [:br]
                      content]
            :time 5000})

(defn no-permission-msg
  "A message telling the user that he does not have permission for something."
  ([reason] (no-permission-msg reason {}))
  ([reason options]
   [sa/Message (merge {:icon "exclamation triangle"
                       :header "Keine Berechtigung."
                       :content reason
                       :warning true}
                      options)]))

(defn select-event-msg
  "A message telling the user to select an event to see the view."
  ([] (select-event-msg {}))
  ([options]
   [sa/Message (merge {:icon "info circle"
                       :header "Wähle ein Event."
                       :content "Zum Anzeigen dieser Ansicht, wähle bitte ein Event aus."}
                      options)]))

(defn logout-forced-msg
  [data]
  #:message{:icon "question circle"
            :title "Du wurdest vom Server ausgeloggt."
            :content [:p "Grund dafür ist wahrscheinlich, dass der Spielleiter das Event beendet hat." [:br]
                      (if config/development? data)]
            :time 10000})

(defn event-not-destroyed-msg
  "A message for 'event could not be deleted' response from server."
  [reason]
  #:message{:icon "exclamation triangle"
            :title "Event konnte nicht gelöscht werden."
            :content [:p "Das gewählte Event konnte nicht gelöscht werden. Technische Informationen:"
                      [:br]
                      reason]})

(defn event-started-msg
  "A message for the case when an event was started successuflly."
  [{:keys [:event/id :event/name]}]
  #:message {:icon "info circle"
             :title (str "Das Event \"" name "\" (" id ") wurde erfolgreich gestartet.")
             :time 3000})

(defn event-not-started-msg
  "A message for the case when an event could net be started."
  [{:as event :keys [:reason :event/id :event/name]}]
  #:message {:icon "exclamation triangle"
             :title (str "Das Event " id " konnte nicht gestartet werden.")
             :content [:p "Technische Informationen: "
                       [:br]
                       (or (:reason event) "Nicht verfügbar.")]})

(defn no-such-event
  "A message indicating that some selected event does not exist."
  []
  #:message {:icon "exclamation triangle"
             :title (str "Das gewählte Event existiert nicht.")
             :content "Es wurde entweder gelöscht oder ein Datenverlust liegt zu Grunde."})

(defn invalid-submission-msg
  "A message for invalid form submissions."
  []
  #:message {:icon "exclamation triangle"
             :title "Bitte eine vollständige und gültige Eingabe tätigen."
             :time 3000})

(defn invalid-game-data-msg
  "A message for invalid game data."
  ([] (invalid-game-data-msg ""))
  ([reason]
   #:message {:icon "exclamation triangle"
              :title "Fehlerhafte Spieldaten."
              :content [:p
                        "Die vom Server empfangenen Spieldaten sind fehlerhaft oder unvollständig."
                        [:br]
                        (str reason)]}))

(defn game-not-yet-started
  "A message indicating that the game has not yet been started by the leader."
  []
  #:message {:icon "info circle"
             :title "Spiel wurde noch nicht gestartet."
             :content [:p "Der Spielleiter hat das Spiel noch nicht gestartet."]})

(defn invalid-round-count
  "A message indicating an out-of-bounds or invalid round-count."
  [round-num]
  #:message {:icon "info circle"
             :title (if (<= round-num 0)
                      "Spiel hat noch nicht angefangen."
                      "Spiel ist vorbei.")})

(defn game-update-failed
  "A message telling the user that a game-update (round-commit) has failed."
  [update-map]
  #:message {:icon "exclamation triangle"
             :title "Spielupdate gescheitert."
             :content [:p "Das Spielupdate konnte nicht durchgeführt werden."
                       "Technische Daten:"
                       [:br]
                       (string/join (take 500 (str (:update/reason update-map))))]})
