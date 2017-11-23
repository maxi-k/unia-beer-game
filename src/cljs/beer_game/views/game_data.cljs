(ns beer-game.views.game-data
  (:require [reagent.core :as ra]
            [re-frame.core :as rf]
            [soda-ash.core :as sa]
            [beer-game.components.messages :as msgs]
            [beer-game.config :as config]
            [beer-game.util :as util]))

(def game-data-columns
  "A map describing the entries for the header rows of a game table.
  Keys are the map keys in a game role-data map, values are the
  respective titles to be displayed."
  #:round{:stock "Lager"
          :cost "Kosten"
          :demand "Nachfrage"
          :incoming "Eingang"})

(defn user-role-title
  [role]
  [:div {:class-name "game-data--user-role-title"}
   [:img {:src (config/user-role->image role)}]
   [:span (config/user-role->title role)]])

(defn game-data-table
  [event-data role-list]
  (fn [{:as event-data
       {:as game-data :keys [:game/rounds :game/current-round]}
       :game/data}]
    (cond
      (nil? event-data)
      [msgs/render-message (msgs/no-such-event)]
      ;; -----
      (not (:event/started? event-data))
      [msgs/render-message (msgs/game-not-yet-started)]
      ;; ------
      :else
      [:div
       (->>
        (for [role role-list]
          [sa/Segment {:key role}
           [sa/Table {:definition true}
            [sa/TableHeader
             [sa/TableRow
              [sa/TableHeaderCell
               [user-role-title role]]
              (for [[key title] game-data-columns]
                [sa/TableHeaderCell {:key key}
                 title])]]
            [sa/TableBody
             (for [[idx {:as round
                         roles :game/roles}]
                   (map-indexed vector (take current-round rounds))
                   :let [round-data (get roles role {})]]
               [sa/TableRow {:key idx}
                [sa/TableCell {:key idx} "Runde " idx]
                (for [[key _] game-data-columns]
                  [sa/TableCell {:key (str idx "-" key)}
                   (get round-data key "-")])])]]])
        (interleave (map (fn [idx]
                           [:div {:key (str "arrow-" idx)
                                  :class-name "game-data--supply-chain-arrow"}
                            [sa/Icon {:name "arrow down"
                                      :size "large"}]])
                         (range)))
        (drop 1)
        (doall))]
      )))

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
        user (rf/subscribe [:user])
        _ (println @user)
        single-event? (util/single-event? (:event/id @user))
        event-cursor (if single-event?
                       (ra/atom (:event/id @user))
                       (ra/wrap @(rf/subscribe [:selected-event])
                                #(rf/dispatch [:event/select %])))]
    (fn []
      [:div
       [:h2 "Spieldetails"]
       (when-not single-event?
         [event-selector event-cursor @events])
       [sa/Divider]
       (if (nil? @event-cursor)
         [msgs/select-event-msg]
         (let [event (get @events @event-cursor)]
           [game-data-table event
            (if (= config/leader-realm (:user/realm @user))
              (get-in event [:game/data :game/settings :game/supply-chain])
              [(:user/role @user)])]))])))
