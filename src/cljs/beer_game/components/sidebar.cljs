(ns beer-game.components.sidebar
  "Components managing the sidebar-menu
  on the left side of the screen."
  (:require [re-frame.core :as rf]
            [soda-ash.core :as sa]
            [beer-game.client-util :as util]
            [beer-game.config :as config]))

(def sidebar-actions
  "The actions on the bottom of the sidebar."
  [{:icon "theme"
    :action #(rf/dispatch [:set-theme])}
   {:icon "log out"
    :title "Logout"
    :action #(rf/dispatch [:auth/logout true])}])

(defn app-menu-link
  "Generic compoonent rendering a link
  in the app menu."
  [{:keys [icon path title]} active]
  [sa/MenuItem {:key path
                :href path
                :icon icon
                :content title
                :link true
                :name title
                :active active}])

(defn app-menu-action
  "Generic component rendering an action
  in the app menu."
  [{:keys [icon title action]}]
  [sa/MenuItem {:key (or title icon)
                :icon icon
                :content title
                :name title
                :link true
                :on-click action
                :position "right"}])

(defn role-string
  "The string that should be displayed for
  the given user-role in the sidebar."
  [{:keys [:user/realm :user/role]}]
  (let [role-title (get-in config/user-roles [(keyword role) :title] "Keine Rolle")]
    (if (= config/leader-realm (keyword realm))
      (str role-title " (" (str (get-in config/realms [realm :title]) ")"))
      role-title)))

(defn app-menu
  "The main component rendering the sidebar.
  Connected to the re-frame store."
  [links active-item]
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
      [sa/MenuItem {:key "header" :header true}
       [:p @title
        [:br]
        (if-let [event-name (get-in @user [:event/data :event/name])]
          [:span "(" event-name ")"])]]
      [sa/MenuItem {:key "player-info"}
       [sa/Icon {:name "user"}]
       [:p "Meine Rolle:" [:br]
        [:strong (role-string @user)]]]
      (for [[key link-item]  links
            :when (map? link-item)]
        ^{:key key}
        [app-menu-link link-item (or (= active-item key)
                                     (and (= :default-panel active-item)
                                          (= (:default-panel links) key)))])]
     [:div.bottom-content
      (map #(app-menu-action %) sidebar-actions)]]))

(def sidebar-width
  "The width of the sidebar in pixels."
  (+ 148 1))
