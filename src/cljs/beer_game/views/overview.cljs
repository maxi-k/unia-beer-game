(ns beer-game.views.overview
  (:require [re-frame.core :as rf]
            [reagent.core :as ra]
            [soda-ash.core :as sa]
            [beer-game.api :as api]
            [beer-game.components.messages :as msgs]
            [beer-game.components.inputs :as inputs]
            [beer-game.config :as config]
            [beer-game.logic.game :as game-logic]
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
    (min (:round/demand round-data)
         (:round/stock round-data))
    ]])

(defn incoming
  "The part of the view that represents the production / incoming items
  for the current player."
  [round-data]
  [game-area {} :incoming
   "Wareneingang"
   [:div.message-data.single-value
    (:round/incoming round-data)]])

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

(defn cost
  "The part of the view that represents the overall cost
  for the rounds given. "
  [rounds user-role]
  [game-area {} :cost
   "Kosten"
   [:div.message-data.single-value
    (game-logic/overall-cost rounds user-role)]])

(defn order
  "The part of the view that represents the request
  this player has for the guy above him in the supply chain."
  []
  (let [form-data (atom {:round/order 0})
        input-data (inputs/make-validated-input
                    {:key "order-input"
                     :class-name "order-input"
                     :spec :round/order
                     :placeholder "Bestellung"
                     :transform js/parseInt
                     :on-change #(swap! form-data assoc :round/order (.-value %2))
                     :value-fn #(:round/order @form-data)
                     :invalid-msg "Bitte eine ganze Zahl eingeben."})
        submit-fn (atom #(rf/dispatch [:game/round-commit @form-data]))]
    (fn []
      [game-area {} :request
       "Anfrage"
       [:div.message-data
        [inputs/validated-form {:as sa/Form
                                :style {:display :block}
                                :validated-inputs [input-data]
                                :submit-atom submit-fn}
         ^{:key :input}
         [inputs/validated-input input-data]
         [sa/Button {:key :btn
                     :primary true
                     :on-click @submit-fn
                     :content "Bestellen"}]]]])))

(defn round-view
  "Renders the game view for the current round."
  [rounds cur-round user-role]
  (let [round-data (get-in (nth rounds cur-round)
                           [:game/roles user-role])]
    [sa/Grid {:class-name "game-view-grid"
              :vertical-align "middle"
              :columns 5}
     [sa/GridRow
      [sa/GridColumn {:width 4}
       [mail round-data]
       [incoming round-data]]
      [sa/GridColumn {:width 1
                      :class-name "game-area-divider"}
       [sa/Icon {:name "arrow right"}]]
      [sa/GridColumn {:width 6}
       [stock round-data]
       [cost (take cur-round rounds) user-role]
       [order]]
      [sa/GridColumn {:width 1
                      :class-name "game-area-divider"}
       [sa/Icon {:name "arrow right"}]]
      [sa/GridColumn {:width 4}
       [outgoing round-data]]]]))

(defn game-view
  "Renders the game view for the current player."
  [user-role]
  (let [game (rf/subscribe [:game])]
    (fn [user-role]
      (let [{:as game-data
             :keys [:game/current-round :game/rounds]} @game]
        (cond
          (not (spec/valid?
                :game/data game-data)) [msgs/render-message
                                        (msgs/invalid-game-data-msg
                                         (spec/explain-str :game/data game-data))]
          (or (>= current-round (count rounds))
              (neg? current-round)) (msgs/render-message
                                     (msgs/invalid-round-count current-round))
          :else
          [round-view rounds current-round user-role])))))

(defn overview-panel
  []
  (let [user (rf/subscribe [:user])
        game (rf/subscribe [:game])
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
                     :subheader (str "Runde " (:game/current-round @game))
                     :text-align :center
                     :as :h1
                     :image img}]
         (if (get-in @events [event-id :event/started?])
           [game-view (:user/role @user)]
           [msgs/render-message (msgs/game-not-yet-started)])])})))
