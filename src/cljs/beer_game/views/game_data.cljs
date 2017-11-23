(ns beer-game.views.game-data
  (:require [reagent.core :as ra]
            [re-frame.core :as rf]
            [soda-ash.core :as sa]
            [beer-game.components.messages :as msgs]))

(defn game-data-table
  [game-data]
  [sa/Table

   ])

(defn event-selector
  [data-atom events]
  (let [options (map
                 (fn [[k {:keys [:event/id :event/name]}]]
                   {:key id :text (str id " - " name) :value id})
                 events)]
    [sa/FormSelect {:options options
                    :placeholder "Event auswählen"
                    :on-change (fn [e val]
                                 (reset! data-atom
                                         (:value (js->clj val
                                                          :keywordize-keys true))))}]))


(defn game-data-panel
  []
  (let [events (rf/subscribe [:events])
        view-data (ra/atom {:selected-event nil})
        event-cursor (ra/cursor view-data [:selected-event])]
    (fn []
      [:div
       [:h2 "Spieldetails"]
       [event-selector event-cursor @events]
       (if (nil? @event-cursor)
         [msgs/select-event-msg]
         [game-data-table (get @events @event-cursor)])])))
