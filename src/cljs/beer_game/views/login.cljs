(ns beer-game.views.login
  (:require [re-frame.core :as rf]
            [soda-ash.core :as sa]
            [reagent.core :as ra]
            [beer-game.client-util :as util]
            [beer-game.util :refer [keyword->string]]
            [beer-game.config :as config]
            [beer-game.components.game-title :as game-title]))

(defn login-button
  [on-click]
  (-> sa/Button
      (#(util/with-options {:primary true
                            :content "Anmelden"
                            :on-click on-click} %))
      util/native-render-fn))

(def user-options
  (reduce
   (fn [coll [key value]]
     (let [k (keyword->string key)]
       (if (not (contains? (:except value) config/player-realm))
         (conj coll {:key k :value k :text (:title value)})
         coll)))
   #{}
   config/user-roles))

(defn login-user-pane []
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

(defn login-leader-pane []
  (let [pw (ra/atom "")]
    (fn []
      [sa/Form
       [sa/FormInput {:label "Passwort"
                      :placeholder "Passwort des Betreuers"
                      :type :password
                      :value @pw
                      :on-change #(reset! pw (.. % -target -value))}]
       [sa/FormField {:control (login-button #(rf/dispatch [:auth/login :realm/leader {:auth/key @pw}]))}]])))

(defn wrap-tab-pane [options child]
  (fn []
    [sa/TabPane options
     [child]]))


(def invalid-credentials-msg
  {:header "Schlüssel ungültig"
   :content "Der eingegebene Schlüssel war nicht gültig."
   :icon "exclamation circle"})

(def successful-logout-msg
  {:header "Ausloggen erfolgreich"
   :content "Du wurdest erfolgreich abgemeldet."
   :icon "check circle"})

(defn auth-message [auth-data]
  (if-let [options (cond
                     (:auth-failure @auth-data) invalid-credentials-msg
                     (:logout-success @auth-data) successful-logout-msg
                     :default nil)]
    [sa/Message (merge {:className "embedded"
                        :attached true} options)]
    [sa/Divider {:horizontal true} "Login"]))


(defn login-card [auth-data]
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
