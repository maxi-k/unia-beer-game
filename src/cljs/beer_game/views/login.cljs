(ns beer-game.views.login
  (:require [re-frame.core :as rf]
            [soda-ash.core :as sa]
            [beer-game.util :as util]))

(defn login-button
  [on-click]
  (-> sa/Button
      (#(util/with-options {:primary true
                            :content "Anmelden"
                            :on-click on-click} %))
      util/native-render-fn))

(defn login-user-pane []
  [sa/Form
   [sa/FormInput {:label "Benutzer ID"
                  :placeholder "Benutzer ID"}]
   [sa/FormField {:control (login-button (fn [] (.log js/console "user")))}]])

(defn login-leader-pane []
  [sa/Form
   [sa/FormInput {:label "Passwort"
                  :placeholder "Passwort des Betreuers"}]
   [sa/FormField {:control (login-button (fn [] (.log js/console "leader")))}]])

(defn wrap-tab-pane [options child]
  (fn []
    [sa/TabPane options
     (child)]))


(defn login-card [name]
  (let [pane-opts {:as "div"}
        wrap-fn (comp util/native-render-fn
                      #(wrap-tab-pane pane-opts %))
        panes [{:menuItem "Benutzer" :render (wrap-fn login-user-pane) }
               {:menuItem "Spielleiter" :render (wrap-fn login-leader-pane)}]]
    (fn []
      [sa/Card {:id "login-card"
                :centered true
                :raised true}
       [sa/CardContent
        [sa/CardHeader
         [:p @name]
         [sa/Icon {:name "beer" :color "yellow" :size :large}]
         [sa/Icon {:name "play circle" :size :large}]]
        [sa/Divider {:horizontal true} "Login"]
        [sa/Container {:text-align :center}
         [sa/Tab {:menu {:secondary true
                         :pointing true
                         :compact true
                         :centered true}
                  :panes panes}]]]])))

(defn login-view
  "View for when the user is not logged in yet."
  []
  (let [name (rf/subscribe [:name])]
    (fn []
      [login-card name])))
