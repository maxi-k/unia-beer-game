(ns beer-game.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [beer-game.util :as util]
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


(defn app-wrapper [& children]
  (let [window-size (re-frame/subscribe [:client/window])]
    [:div#app-wrapper {:style @window-size}
     (util/keyify children)]))

(defn main-panel []
  (let [active-panel (re-frame/subscribe [:active-panel])
        window-size (re-frame/subscribe [:client/window])
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
         [login/login-view])])))
