(ns beer-game.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [beer-game.util :as util]
            [soda-ash.core :as sa]
            [beer-game.components.sidebar :refer [app-menu]]
            [beer-game.views.overview :as overview]
            [beer-game.views.statistics :as statistics]))

(def panels
  {:home-panel {:title "Übersicht"
                :path "#/"
                :icon "dashboard"
                :comp overview/overview-panel}
   :statistics-panel {:title "Statistiken"
                      :path "#/statistics"
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
  (let [active-panel (re-frame/subscribe [:active-panel])]
    (fn []
      [app-wrapper
       [sa/SidebarPushable
        [app-menu panels @active-panel]
        [sa/SidebarPusher
         [:main#main-content-wrapper
          [show-panel @active-panel]]]]])))
