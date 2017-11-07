(ns beer-game.view
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [beer-game.client-util :as util]
            [soda-ash.core :as sa]
            [beer-game.components.sidebar :refer [app-menu sidebar-width]]
            [beer-game.components.messages :as msgs]
            [beer-game.components.animation :as anim]
            [beer-game.config :as config]
            [beer-game.views.overview :as overview]
            [beer-game.views.statistics :as statistics]
            [beer-game.views.events :as events-view]
            [beer-game.views.login :as login]
            [beer-game.views.imprint :as imprint]
            [re-frame.core :as rf]
            [reagent.core :as ra]))

(defn user->panels
  [{:as user-data :keys [:user/realm]}]
  (let [default-panels
        {:statistics-panel {:title "Statistiken"
                            :path "#statistics"
                            :icon "line graph"
                            :comp statistics/statistics-panel}
         :imprint-panel {:title "Impressum"
                         :path "#imprint"
                         :icon "law"
                         :comp imprint/imprint-panel}}
        player-panels
        {:overview-panel {:title "Übersicht"
                          :path "#overview"
                          :icon "dashboard"
                          :comp overview/overview-panel}
         :default-panel :overview-panel}
        leader-panels
        {:events-panel {:title "Events"
                        :path "#events"
                        :icon "users"
                        :comp events-view/events-panel
                        :auth-fn #(= (:user/realm %)
                                     config/leader-realm)}
         :default-panel :events-panel}]
    (if (= realm config/leader-realm)
      (merge leader-panels default-panels)
      (merge player-panels default-panels))))

(defn show-panel [panels panel-name user-data]
  (let [;; Recursively look up the panel to show
        active-panel (loop [selected-panel panel-name]
                       (if (keyword? selected-panel)
                         (recur (get panels selected-panel :default-panel))
                         selected-panel))
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

(defn connection-state []
  (let [connected? (re-frame/subscribe [:client/connected])]
    (fn []
      (if @connected?
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
          [:strong "Lass es am Besten den Spielleiter wissen!"]]]))))

(defn messages
  []
  (let [msgs (re-frame/subscribe [:messages])]
    (fn []
      (anim/transition-group
       {:class "messages"}
       (for [[msg-id msg] @msgs
             :let [{:keys [:message/title
                           :message/content
                           :message/icon]} msg]]
         [sa/Message {:key msg-id
                      :header title
                      :content (if (vector? content)
                                 (ra/as-element content)
                                 (str content))
                      :icon icon
                      :onDismiss #(rf/dispatch [:message/remove msg-id])}])))))

(defn app-wrapper [& children]
  [:div#app-wrapper
   (util/keyify children)])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [:active-panel])
        window-size (re-frame/subscribe [:client/window])
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
          [messages]
          [:div.right-floated
           [connection-state]]]]))))
