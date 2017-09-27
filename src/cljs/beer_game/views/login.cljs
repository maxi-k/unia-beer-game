(ns beer-game.views.login
  (:require [re-frame.core :as rf]
            [soda-ash.core :as sa]
            [reagent.core :as ra]
            [beer-game.client-util :as util]
            [beer-game.config :as config]))

(defn login-button
  [on-click]
  (-> sa/Button
      (#(util/with-options {:primary true
                            :content "Anmelden"
                            :on-click on-click} %))
      util/native-render-fn))

(def user-options
  (map
   (fn [[k v]]
     {:key k :value k :text (:title v)})
   config/user-ids))

(defn login-user-pane []
  (let [pw (ra/atom "")]
    (fn []
      [sa/Form
       [sa/FormSelect {:label "Rolle"
                       :placeholder "Rolle"
                       :value @pw
                       :options user-options
                       :on-change #(reset! pw (.-value %2))}]
       [sa/FormField {:control (login-button (fn []
                                               (rf/dispatch [:auth/login :player @pw])))}]])))

(defn login-leader-pane []
  (let [pw (ra/atom "")]
    (fn []
      [sa/Form
       [sa/FormInput {:label "Passwort"
                      :placeholder "Passwort des Betreuers"
                      :type :password
                      :value @pw
                      :on-change #(reset! pw (.. % -target -value))}]
       [sa/FormField {:control (login-button (fn []
                                               (rf/dispatch [:auth/login :leader @pw])))}]])))

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
   :content "Sie wurden erfolgreich abgemeldet."
   :icon "check circle"})

(defn auth-message [auth-data]
  (if-let [options (cond
                     (:auth-failure @auth-data) invalid-credentials-msg
                     (:logout-success @auth-data) successful-logout-msg
                     :default nil)]
    [sa/Message (merge {:className "embedded"
                        :attached true} options)]
    [sa/Divider {:horizontal true} "Login"]))


(defn login-card [name auth-data]
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
        [:h3 @name]
        [sa/Icon {:name "beer" :color "yellow" :size :large}]
        [sa/Icon {:name "play circle" :size :large}]]
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
  (let [name (rf/subscribe [:name])
        auth-data (rf/subscribe [:user])]
    (fn []
      [:div.login-card-wrapper
       [login-card name auth-data]])))
