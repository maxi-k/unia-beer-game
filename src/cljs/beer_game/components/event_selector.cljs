(ns beer-game.components.event-selector
  "Components for selecting (domain) events."
  (:require [soda-ash.core :as sa]
            [re-frame.core :as rf]
            [beer-game.util :as util]
            [reagent.core :as ra]))

(defn event-selector
  "A drop-down select element for events, taking the current event-id
  `value`, a `on-change` handler which is passed the selected event-id,
  and the list of `events` to display as arguments."
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
  "Like `event-selector`, but taking its arguments directly from
  the re-frame store."
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
