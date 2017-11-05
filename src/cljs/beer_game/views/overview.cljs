(ns beer-game.views.overview
  (:require [re-frame.core :as rf]
            [reagent.core :as ra]
            [soda-ash.core :as sa]
            [beer-game.api :as api]
            [beer-game.components.messages :as msgs]
            [beer-game.config :as config]
            [clojure.spec.alpha :as spec]
            [beer-game.spec.game]))

(defn game-area
  ([options area title children]
   (let [area-str (name area)]
     [sa/SegmentGroup (merge {:class-name (str "game-area " area-str)}
                             options)
      [sa/Segment {:class-name "title"}
       [:h3 title]]
      [sa/Segment {:class-name "content-wrapper"}
       [:div.content {:style {:background-image
                              (str "url(" config/game-area-path "/" area-str ".svg" )}}
        children]]])))

(defn outgoing
  "The part of the view that represents the outgoing items
  for the current-player for the previous round."
  [round-data]
  [game-area {} :outgoing
   "Warenausgang"
   [:div.message-data.single-value
    (:round/demand round-data)
    ]])

(defn incoming
  "The part of the view that represents the production / incoming items
  for the current player."
  [round-data]
  [game-area {} :incoming
   "Wareneingang"
   [:div.message-data.single-value
    (:round/order round-data)]])

(defn mail
  "The part of the view that represents the mailbox of the current player."
  [round-data]
  [game-area {} :mail
   "Nachrichten"
   [:div.message-data
    "Angefragt:"
    [:br]
    [:span.main-value
     (:round/demand round-data)]]])

(defn stock
  "The part of the view that represents the stock of the current player.
  For the round given."
  [round-data]
  [game-area {} :stock
   "Lager"
   [:div.message-data.single-value
    (:round/stock round-data)]])

(defn round-view
  "Renders the game view for the current round."
  [cur-round user-role]
  (let [round-data (get-in cur-round [:game/roles user-role])]
    [sa/Grid {:class-name "game-view-grid"
              :vertical-align "middle"
              :columns 3}
     [sa/GridRow
      [sa/GridColumn
       [mail round-data]
       [incoming round-data]]
      [sa/GridColumn
       [stock round-data]]
      [sa/GridColumn
       [outgoing round-data]]]]))

(defn game-view
  "Renders the game view for the current player."
  [user-role]
  (let [{:as game-data
         :keys [:game/current-round :game/rounds]}
        @(rf/subscribe [:game])
        _ (println game-data)]
    (fn [user-role]
      (cond
        (not (spec/valid?
              :game/data game-data)) [msgs/render-message
                                      (msgs/invalid-game-data-msg
                                       (spec/explain-str :game/data game-data))]
        (or (>= current-round (count rounds))
            (neg? current-round)) (msgs/render-message
                                   (msgs/invalid-round-count current-round))
        :else
        (let [cur-round (nth rounds current-round)]
          [round-view cur-round user-role])))))

(defn overview-panel
  []
  (let [user (rf/subscribe [:user])
        role (:user/role @user)
        img (config/user-role->image role)
        events (rf/subscribe [:events])
        event-id (:event/id @user)]
    (ra/create-class
     {:component-did-mount #(rf/dispatch [:game/data-fetch])
      :reagent-render
      (fn []
        [sa/Container {:class-name "game-wrapper"}
         [sa/Header {:class-name "role-title"
                     :content (config/user-role->title role)
                     :text-align :center
                     :as :h1
                     :image img}]
         (if (get-in @events [event-id :event/started?])
           [game-view (:user/role @user)]
           [msgs/render-message (msgs/game-not-yet-started)])])})))
