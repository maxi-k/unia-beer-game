(ns beer-game.views.login
  "View for logging in. Also the standard view if
  the user is not logged in."
  (:require [re-frame.core :as rf]
            [soda-ash.core :as sa]
            [reagent.core :as ra]
            [beer-game.client-util :as util]
            [beer-game.util :refer [keyword->string]]
            [beer-game.config :as config]
            [beer-game.components.game-title :as game-title]))

(defn login-button
  "The button triggering the login action."
  [on-click]
  (-> sa/Button
      (#(util/with-options {:primary true
                            :content "Anmelden"
                            :on-click on-click} %))
      util/native-render-fn))

(def user-options
  "The list of user-roles available for login for players,
  formated as someting that can be passed to a select."
  (reduce
   (fn [coll [key value]]
     (let [k (keyword->string key)]
       (if (not (contains? (:except value) config/player-realm))
         (conj coll {:key k :value k :text (:title value)})
         coll)))
   #{}
   config/user-roles))

(defn login-user-pane
  "The part of the user login that is for players."
  []
  (let [data (ra/atom {:event/id ""
                       :auth/key ""})]
    (fn []
      [sa/Form
       [sa/FormInput {:label "Event ID"
                      :placeholder "Die Event ID wird vom Spielleiter bekannt gegeben"
                      :value (:event/id @data)
                      :on-change #(swap! data assoc :event/id (.-value %2))}]
       [sa/FormSelect {:label "Rolle"
                       :placeholder "Rolle auswählen"
                       :value (:auth/key @data)
                       :options user-options
                       :on-change #(swap! data assoc :auth/key (.-value %2))}]
       [sa/FormField {:control (login-button #(rf/dispatch [:auth/login :realm/player @data]))}]])))

(defn login-leader-pane
  "The part of the user login that is for leaders."
  []
  (let [pw (ra/atom "")]
    (fn []
      [sa/Form
       [sa/FormInput {:label "Passwort"
                      :placeholder "Passwort des Betreuers"
                      :type :password
                      :value @pw
                      :on-change #(reset! pw (.. % -target -value))}]
       [sa/FormField {:control (login-button #(rf/dispatch [:auth/login :realm/leader {:auth/key @pw}]))}]])))

(defn wrap-tab-pane
  "Wraps the given child in a tab pane for selecting."
  [options child]
  (fn []
    [sa/TabPane options
     [child]]))


(def invalid-credentials-msg
  "The definition for the message indicating that the login
  credentials were invalid."
  {:header "Schlüssel ungültig"
   :content "Der eingegebene Schlüssel war nicht gültig."
   :icon "exclamation circle"})

(def invalid-event-msg
  "The definition for the message indicating that the given
  event-id does not exist."
  {:header "Event-ID ungültig"
   :content "Das gewählte Event existiert nicht."
   :icon "exclamation circle"})

(def event-started-msg
  "Message indicating that the selected event has started already
  without the selected role."
  {:header "Event läuft ohne gewählte Rolle"
   :content "Das gewählte Event wurde bereits ohne die gewählte Rolle gestartet."
   :icon "info circle"})

(def successful-logout-msg
  "Message indicating that the logout was successful."
  {:header "Ausloggen erfolgreich"
   :content "Du wurdest erfolgreich abgemeldet."
   :icon "check circle"})

(defn auth-message
  "Displays a message indicating the result of the login/logout action."
  [auth-data]
  (if-let [options (cond
                     (:auth-failure @auth-data)
                     (condp #(contains? %2 %1) (:auth-failure-reason @auth-data)
                       :user/role event-started-msg
                       :event/id invalid-event-msg
                       invalid-credentials-msg)
                     (:logout-success @auth-data) successful-logout-msg
                     :default nil)]
    [sa/Message (merge {:className "embedded"
                        :attached true} options)]
    [sa/Divider {:horizontal true} "Login"]))


(defn login-card
  "The card that contains all of the login view (player, leader, messages...)
  and the logo."
  [auth-data]
  (let [pane-opts {:as "div"}
        wrap-fn (comp util/native-render-fn
                      #(wrap-tab-pane pane-opts %))
        panes [{:menuItem "Mitspieler" :render (wrap-fn login-user-pane) }
               {:menuItem "Spielleiter" :render (wrap-fn login-leader-pane)}]]
    (fn []
      [sa/Card {:class "login-card"
                :centered true
                :raised true}
       [sa/CardHeader
        [:h3 {:class-name "login-title"}
         [game-title/game-title-split :p]]]
       [auth-message auth-data]
       [sa/CardContent
        [sa/Container {:text-align :center}
         [sa/Tab {:menu {:secondary true
                         :pointing true
                         :compact true}
                  :panes panes}]]]])))

(defn login-view
  "View for when the user is not logged in yet."
  []
  (let [auth-data (rf/subscribe [:user])]
    (fn []
      [:div.login-card-wrapper
       [login-card auth-data]])))
