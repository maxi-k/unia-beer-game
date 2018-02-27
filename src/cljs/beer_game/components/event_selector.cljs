(ns beer-game.components.event-selector
  (:require [soda-ash.core :as sa]
            [re-frame.core :as rf]
            [beer-game.util :as util]
            [reagent.core :as ra]))

(defn event-selector
  [value on-change events]
  (let [options (map
                 (fn [[k {:keys [:event/id :event/name]}]]
                   {:key id :text (str id " - " name) :value id})
                 events)]
    [sa/FormSelect {:options options
                    :value value
                    :placeholder "Event auswählen"
                    :on-change (fn [e val]
                                 (on-change
                                  (:value (js->clj val
                                                   :keywordize-keys true))))}]))

(defn connected-event-selector
  []
  (let [events (rf/subscribe [:events])
        user (rf/subscribe [:user])
        single-event? (util/single-event? (:event/id @user))
        selected-event (if single-event?
                         (ra/cursor user [:event/id])
                         (rf/subscribe [:selected-event]))
        select-on-change (if single-event? identity #(rf/dispatch [:event/select %]))]
    (fn []
      (when-not single-event?
        [event-selector @selected-event select-on-change @events]))))
