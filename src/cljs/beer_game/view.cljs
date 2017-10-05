(ns beer-game.view
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [beer-game.client-util :as util]
            [soda-ash.core :as sa]
            [beer-game.components.sidebar :refer [app-menu sidebar-width]]
            [beer-game.components.messages :as msgs]
            [beer-game.config :as config]
            [beer-game.views.overview :as overview]
            [beer-game.views.statistics :as statistics]
            [beer-game.views.events :as events-view]
            [beer-game.views.login :as login]
            ))

(defn user->panels
  [{:as user-data :keys [:user/realm]}]
  (let [default-panels
        {:overview-panel {:title "Übersicht"
                          :path "#overview"
                          :icon "dashboard"
                          :comp overview/overview-panel}
         :statistics-panel {:title "Statistiken"
                            :path "#statistics"
                            :icon "line graph"
                            :comp statistics/statistics-panel}}
        leader-panels
        {:events-panel {:title "Events"
                        :path "#events"
                        :icon "users"
                        :comp events-view/events-panel
                        :auth-fn #(= (:user/realm %)
                                     config/leader-realm)}}]
    (if (= realm config/leader-realm)
      (merge default-panels leader-panels)
      default-panels)))

(defn show-panel [panels panel-name user-data]
  (let [active-panel (get panels panel-name {:comp :div})
        auth-fn (get active-panel :auth-fn (constantly true))]
    (if (auth-fn user-data)
      [(:comp active-panel)]
      (msgs/no-permission-msg
       "Du hast leider keine Berechtigung für diese Seite."))))

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
    [sa/Popup {:content "Mit dem Server verbunden."
               :hoverable true
               :trigger (connection-widget :green "podcast" {})}]
    [sa/Popup {:trigger (connection-widget :red "exclamation"
                                           {:class-name "attention"})
               :hoverable true}
     [sa/PopupHeader "Keine Verbindung zum Server."]
     [sa/PopupContent
      "Es kann im Moment keine Verbindung zum Beer-Game Server hergestellt werden."
      [:br]
      [:strong "Lass es am Besten den Spielleiter wissen!"]]]))

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
      (let [panels (user->panels @user-data)]
        [app-wrapper
         (if (:auth @user-data)
           [sa/SidebarPushable
            [app-menu panels @active-panel]
            [sa/SidebarPusher {:style {:width (str (- (:width @window-size) sidebar-width)
                                                   "px")
                                       :min-width "500px"}}
             [:main#main-content-wrapper
              [show-panel panels @active-panel @user-data]]]]
           [login/login-view])
         [:div.system-message-tray
          [connection-state @connection]]]))))
