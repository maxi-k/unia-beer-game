(ns beer-game.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [beer-game.client-util :as util]
            [soda-ash.core :as sa]
            [beer-game.components.sidebar :refer [app-menu sidebar-width]]
            [beer-game.views.overview :as overview]
            [beer-game.views.statistics :as statistics]
            [beer-game.views.login :as login]))

(def panels
  {:overview-panel {:title "Übersicht"
                    :path "#overview"
                    :icon "dashboard"
                    :comp overview/overview-panel}
   :statistics-panel {:title "Statistiken"
                      :path "#statistics"
                      :icon "line graph"
                      :comp statistics/statistics-panel}})

(defn show-panel [panel-name]
  [(-> panels
       (get panel-name {:comp :div})
       :comp)])

(defn connection-widget
  [color icon options]
  (->
   sa/Icon
   (#(util/with-options (merge {:circular true
                                :name icon
                                :fitted true
                                :inverted true
                                :color color
                                :size :large} options) %))
   util/native-component))

(defn connection-state [connected?]
  (if connected?
    [sa/Popup {:header "Mit dem Server verbunden."
               :hoverable true
               :trigger (connection-widget :green "podcast" {})}]
    [sa/Popup {:trigger (connection-widget :red "exclamation" {:class-name "attention"})
               :hoverable true}
     [sa/PopupHeader "Keine Verbindung zum Server."]
     [sa/PopupContent
      "Es kann im Moment keine Verbindung zum Beer-Game Server hergestellt werden."
      [:br]
      [:strong "Am besten dem Spielleiter sagen!"]]]))

(defn app-wrapper [& children]
  (let [window-size (re-frame/subscribe [:client/window])]
    [:div#app-wrapper {:style @window-size}
     (util/keyify children)]))

(defn main-panel []
  (let [active-panel (re-frame/subscribe [:active-panel])
        window-size (re-frame/subscribe [:client/window])
        connection (re-frame/subscribe [:client/connected])
        user-data (re-frame/subscribe [:user])]
    (fn []
      [app-wrapper
       (if (:auth @user-data)
         [sa/SidebarPushable
          [app-menu panels @active-panel]
          [sa/SidebarPusher {:style {:width (str (- (:width @window-size) sidebar-width)
                                                 "px")
                                     :min-width "500px"}}
           [:main#main-content-wrapper
            [show-panel @active-panel]]]]
         [login/login-view])
       [:div.system-message-tray
        [connection-state @connection]]])))
