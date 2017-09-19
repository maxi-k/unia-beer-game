(ns beer-game.components.sidebar
  (:require [re-frame.core :as rf]
            [soda-ash.core :as sa]
            [beer-game.util :as util]
            [beer-game.config :as config]))

(def sidebar-actions
  [{:icon "theme"
    :action #(rf/dispatch [:set-theme])}
   {:icon "log out"
    :title "Logout"
    :action #(rf/dispatch [:auth/logout true])}])

(defn app-menu-link [{:keys [icon path title]} active]
  [sa/MenuItem {:key path
                :href path
                :icon icon
                :content title
                :link true
                :name title
                :active active}])

(defn app-menu-action [{:keys [icon title action]}]
  [sa/MenuItem {:key (or title icon)
                :icon icon
                :content title
                :name title
                :link true
                :on-click action
                :position "right"}])

(defn role-string [{:keys [realm uid]}]
  (let [uid-title (get-in config/user-ids [(keyword uid) :title] "Keine Rolle")]
    (if (= config/leader-realm (keyword realm))
      (str uid-title " (" (str (get-in config/realms [realm :title]) ")"))
      uid-title)))

(defn app-menu [links active-item]
  (let [color (rf/subscribe [:client/theme])
        user (rf/subscribe [:user])
        title (rf/subscribe [:name])]
    [sa/Sidebar
     {:as (util/semantic-to-react sa/Menu)
      :id "app-menu"
      :animation :push
      :visible true
      :icon true
      :inverted (= :dark @color)
      :vertical true
      :size "huge"
      :width "thin"}
     [:div.top-content
      [sa/MenuItem {:key "header" :header true} @title]
      [sa/MenuItem {:key "player-info"}
       [sa/Icon {:name "user"}]
       [:p [:strong "Meine Rolle: "]
        (role-string @user)]]
      (map #(app-menu-link (% 1) (= active-item (% 0))) links)]
     [:div.bottom-content
      (map #(app-menu-action %) sidebar-actions)]]))

(def sidebar-width
  "The width of the sidebar in pixels."
  (+ 148 1))
