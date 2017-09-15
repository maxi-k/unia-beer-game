(ns beer-game.views.login
  (:require [re-frame.core :as rf]
            [soda-ash.core :as sa]
            [beer-game.util :as util]))

(defn login-user-pane []
  [sa/TabPane {:attached false
               :as "div"}
   "User"])

(defn login-leader-pane []
  [sa/TabPane {:attached false
               :as "div"}
   "Leader"])

(defn login-card [name]
  [sa/Card {:id "login-card"
            :centered true
            :raised true}
   [sa/CardContent
    [sa/CardHeader @name]
    [sa/Tab {:menu {:secondary true
                    :pointing true}
             :panes [{:menuItem "Benutzer" :render (util/native-render-fn login-user-pane) }
                     {:menuItem "Spielleiter" :render (util/native-render-fn login-leader-pane)}]}]]])

(defn login-view
  "View for when the user is not logged in yet."
  []
  (let [name (rf/subscribe [:name])]
    (fn []
      [login-card name]
      )))
