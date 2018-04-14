(ns beer-game.views.game-data
  "View for the tables displaying all sorts of data around a running game."
  (:require [reagent.core :as ra]
            [re-frame.core :as rf]
            [soda-ash.core :as sa]
            [beer-game.components.messages :as msgs]
            [beer-game.components.event-selector :as selector]
            [beer-game.config :as config]
            [beer-game.util :as util]))

(def game-data-player-columns
  "A map describing the entries for the header rows of a game table.
  Keys are the map keys in a game role-data map, values are the
  respective titles to be displayed.
  This is the one for all players."
  #:round{:stock ["Lager" "(Stück, Gesamt)"]
          :cost ["Kosten" "(Dollar, je Runde)"]
          :demand ["Warennachfrage" "(Stück, je Runde)"]
          :incoming ["Wareneingang" "(Stück, je Runde)"]})

(def game-data-customer-columns
  "A map describing the entries for the header rows of a game table.
  Keys are the map keys in a game role-data map, values are the
  respective titles to be displayed.
  This is the one for the customer (the computer)."
  #:round{:order ["Bestellung" "(Stück, je Runde)"]
          :incoming ["Wareneingang" "(Stück, je Runde)"]})

(defn user-role-title
  "Displays the title for a role displayed in the top left
  corner of the game-data table."
  [role]
  [:div {:class-name "game-data--user-role-title"}
   [:img {:src (config/user-role->image role)}]
   [:span (config/user-role->title role)]
   [:div {:class-name "clearfloat"}]])

(defn game-data-table
  "Component for the table which contains the game-data for a
  running game. Displays the data associated with all the roles in `role-list`."
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
        (for [role role-list
              :let [columns (if (= role config/customer-role)
                              game-data-customer-columns
                              game-data-player-columns)]]
          [sa/Segment {:key role}
           [sa/Table {:definition true :compact true}
            [sa/TableHeader
             [sa/TableRow
              [sa/TableHeaderCell {:width 5}
               [user-role-title role]]
              (for [[key title] columns]
                [sa/Popup
                 {:key key
                  :content (config/round-property->description key)
                  :hoverable true
                  :trigger
                  (ra/as-element
                   [sa/TableHeaderCell {:key key}
                    [:div {:class-name "game-data--role-property-header-text"}
                     (if (vector? title)
                       (for [item title]
                         [:span {:key item} item])
                       title)]
                    [sa/Icon {:class-name "game-data--role-property-header-info"
                              :name "question circle outline"}]])}])]]
            [sa/TableBody
             (for [[idx {:as round
                         roles :game/roles}]
                   (map-indexed vector (take current-round rounds))
                   :let [round-data (get roles role {})]]
               [sa/TableRow {:key idx}
                [sa/TableCell {:key idx} "Runde " idx]
                (for [[key _] columns]
                  [sa/TableCell {:key (str idx "-" key)}
                   (get round-data key "-")])])]]])
        (util/interpose-indexed
         (fn [idx]
           [:div {:key (str "arrow-" idx)
                  :class-name "game-data--supply-chain-arrow"}
            [sa/Icon {:name "arrow down"
                      :size "large"}]]))
        (doall))])))


(defn game-data-panel
  "Panel that wraps the game selector an the table for the role(s)
  and their game data. For any given player, only their own data
  is displayed, while the leaders sees the data associated with all roles."
  []
  (let [events (rf/subscribe [:events])
        user (rf/subscribe [:user])
        single-event? (util/single-event? (:event/id @user))
        selected-event (if single-event?
                         (ra/cursor user [:event/id])
                         (rf/subscribe [:selected-event]))
        select-on-change (if single-event? identity #(rf/dispatch [:event/select %]))]
    (fn []
      [:div
       [:h2 "Spieldetails"]
       (when-not single-event?
         [selector/event-selector @selected-event select-on-change @events])
       [sa/Divider]
       (if (nil? @selected-event)
         [msgs/select-event-msg]
         (let [event (get @events @selected-event)]
           [game-data-table event
            (if (= config/leader-realm (:user/realm @user))
              (get-in event [:game/data :game/settings :game/supply-chain])
              [(:user/role @user)])]))])))
