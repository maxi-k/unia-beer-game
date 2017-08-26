(ns beer-game.components.sidebar
  (:require [re-frame.core :as rf]
            [soda-ash.core :as sa]))

(def sidebar-actions
  [{:icon "theme"
    :action #(rf/dispatch [:set-theme])}])

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

(defn app-menu [links active-item]
  (let [color (rf/subscribe [:client/theme])
        title (rf/subscribe [:name])]
    [sa/Sidebar
     {:as (.-name sa/Menu)
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
      (map #(app-menu-link (% 1) (= active-item (% 0))) links)]
     [:div.bottom-content
      (map #(app-menu-action %) sidebar-actions)]]))
