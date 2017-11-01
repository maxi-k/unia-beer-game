(ns beer-game.views.overview
  (:require [re-frame.core :as rf]
            [soda-ash.core :as sa]
            [beer-game.api :as api]
            [beer-game.components.messages :as msgs]
            [beer-game.config :as config]
            [clojure.spec.alpha :as spec]
            [beer-game.spec.game]))

(defn outgoing
  "The part of the view that represents the outgoing items
  for the current-player"
  [round-data])

(defn incoming
  "The part of the view that represents the production / incoming items
  for the current player."
  [round-data]
  [:div "Outgoging"])

(defn mail
  "The part of the view that represents the mailbox of the current player."
  [round-data]
  [:div "Mail"])

(defn stock
  "The part of the view that represents the stock of the current player.
  For the round given."
  [round-data]
  [:div "Stock"])

(defn round-view
  "Renders the game view for the current round."
  [round-data round-num]
  (let [cur-round (nth round-data round-num)]
    [sa/Grid {:class-name "game-view-grid"}
     [sa/GridColumn
      [sa/GridRow
       [mail cur-round]]
      [sa/GridRow
       [incoming cur-round]]]
     [sa/GridColumn
      [stock cur-round]]
     [sa/GridColumn
      [outgoing cur-round]]]))

(defn game-view
  "Renders the game view for the current player."
  [{:as game-data
    :keys [:game/current-round :game/rounds]}]
  (if (spec/valid? :game/data game-data)
    [round-view rounds current-round]
    [msgs/render-message (msgs/invalid-game-data-msg)]))

(defn overview-panel []
  (let [user (rf/subscribe [:user])
        role (:user/role @user)
        img (config/user-role->image role)
        game (rf/subscribe [:game])]
    [sa/Container {:class-name "game-wrapper"
                   :text true}
     [sa/Header {:class-name "role-title"
                 :content (config/user-role->title role)
                 :text-align :center
                 :as :h1
                 :image img}]
     [game-view game]]))
