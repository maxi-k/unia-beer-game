(ns beer-game.views.overview
  (:require [re-frame.core :as rf]
            [reagent.core :as ra]
            [soda-ash.core :as sa]
            [beer-game.api :as api]
            [beer-game.util :as util]
            [beer-game.client-util :as cutil]
            [beer-game.config :as config]
            [beer-game.components.messages :as msgs]
            [beer-game.components.inputs :as inputs]
            [beer-game.config :as config]
            [beer-game.logic.game :as game-logic]
            [clojure.spec.alpha :as spec]
            [beer-game.spec.game]))

(defn user-role-image
  "Renders the image for a specific user role"
  [{:as options :keys [user-role as-elem as-content no-text]}]
  (let [as (or as-elem sa/Header)]
    [as (merge {:key (str user-role)
                :style {:text-align :center}}
               (dissoc options :user-role :no-text :as-elem :as-content))
     [sa/Image {:src (config/user-role->image user-role)}]
     (when-not no-text
       [(or as-content :span)
        (config/user-role->title user-role)])]))

(defn game-view-header
  "Renders the header at the top of the game view,
  where the supply-chain is displayed."
  [game role]
  (fn [game role]
    (let [supply-chain (or (get-in game [:game/settings :game/supply-chain])
                           config/supply-chain)]
      [sa/Header {:class-name "game-view-header"}
       [:div.game-view-chain
        (->>
         (for [member supply-chain]
           ^{:key member}
           [user-role-image
            {:key member
             :user-role member
             :class-name (if (= role member)
                           (str "role-title active")
                           (str "role-title inactive"))
             :as-elem sa/Header
             :as-content sa/HeaderContent
             :as :h2}])
         (util/interpose-indexed
          (fn [idx]
            ^{:key (str "arrow-" idx)}
            [:div.role-title.arrow {:key (str "arrow-" idx)}
             [sa/Icon {:name "arrow right"
                       :size "large"}]]))
         (doall))
        [:div.clearfloat]]
       [sa/Divider {:class-name "game-view-header-subtitle"
                    :horizontal true}
        (str "Runde " (:game/current-round game))]])))

(defn game-area
  ([options area title child]
   (let [area-str (name area)]
     [sa/SegmentGroup (merge {:class-name (str "game-area " area-str)}
                             options)
      [sa/Segment {:class-name "title"}
       [:h3 title]]
      [sa/Segment {:class-name "content-wrapper"}
       [:div.content {:style {:background-image
                              (str "url(" config/game-area-path "/" area-str ".svg" )}}
        child]]])))

(defn unit-text
  "A component that renders the text for a unit,
  to give the numeric values context."
  [text]
  [:p.unit-text {:key "unit-text"} text])

(defn outgoing
  "The part of the view that represents the outgoing items
  for the current-player for the previous round."
  [round-data]
  [game-area {} :outgoing
   "Warenausgang"
   [:div.message-data
    [:span.main-value
     (min (:round/demand round-data)
          (:round/stock round-data))]
    [unit-text "Einheiten"]]])

(defn incoming
  "The part of the view that represents the production / incoming items
  for the current player."
  [round-data]
  [game-area {} :incoming
   "Wareneingang"
   [:div.message-data
    [:span.main-value
     (:round/incoming round-data)]
    [unit-text "Einheiten"]]])

(defn mail
  "The part of the view that represents the mailbox of the current player."
  [round-data]
  [game-area {} :mail
   "Nachrichten"
   [:div.message-data
    "Angefragt:"
    [:br]
    [:span.main-value
     (:round/demand round-data)]
    [unit-text "Einheiten"]]])

(defn stock
  "The part of the view that represents the stock of the current player.
  For the round given."
  [round-data]
  [game-area {} :stock
   "Lager"
   [:div.message-data
    [:span.main-value
     (:round/stock round-data)]
    [unit-text "Einheiten"]]])

(defn cost
  "The part of the view that represents the overall cost
  for the rounds given. "
  [rounds user-role]
  [game-area {} :cost
   "Kosten"
   [:div.message-data
    [:span.main-value
     (game-logic/overall-cost rounds user-role)]
    [unit-text "Dollar"]]])

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
         ^{:key :input} [inputs/validated-input input-data]
         ^{:key :units} [unit-text "Einheiten"]
         [sa/Button {:key :btn
                     :primary true
                     :on-click @submit-fn
                     :content "Bestellen"}]]]])))

(defn info-group-icon
  "The icon hinting at the purpose of a company-section."
  [icon-name description]
  [sa/Popup
   {:content description
    :trigger (->> sa/Label
                  (cutil/with-options
                    {:icon icon-name
                     :corner "right"})
                  cutil/native-component)}])

(defn info-group
  "Renders one logical group of information as a grid row.
  Requires an icon and a description, a option map, as well as a `children` map
  from react-key -> value."
  [icon-name description options children]
  [sa/GridRow (merge {:as (cutil/semantic-to-react sa/Segment)
                      :class-name ""}
                     options)
   [info-group-icon icon-name description]
   (for [[key elem] children]
     (cutil/with-options-raw {:key key} elem))])

(defn grid-arrow-column
  "Renders a grid cloumn designated to an arrow."
  [{:as opts
    :keys [direction width]
    :or {direction "right" width 1}}]
  [sa/GridColumn (merge {:width width
                         :class-name "game-area-divider"}
                        (dissoc opts :direction))
   [sa/Icon {:name (str "arrow " direction)}]])

(defn round-view
  "Renders the game view for the current round."
  [rounds cur-round user-role supply-chain]
  (let [round-data (get-in (nth rounds cur-round)
                           [:game/roles user-role])
        [supplier customer] (game-logic/roles-around user-role supply-chain)]
    [sa/Grid {:class-name "game-view-grid"
              :vertical-align "middle"
              :columns 5}
     [info-group
      "inbox"
      "Der Informationsfluss Deiner Firma."
      {}
      {:supplier [sa/GridColumn {:width 2} [user-role-image
                                            {:key (or supplier user-role)
                                             :user-role (or supplier user-role)
                                             :no-text true
                                             :class-name "user-role-segment"
                                             :as-elem :div}]]
       :arrow-out [grid-arrow-column {:direction "left"}]
       :outgoing [sa/GridColumn {:width 5} [order]]
       :divider [sa/Divider {:vertical true}]
       :demand [sa/GridColumn {:width 5} [mail round-data]]
       :arrow-in [grid-arrow-column {:direction "left"}]
       :customer [sa/GridColumn {:width 2} [user-role-image
                                            {:key (or supplier user-role)
                                             :user-role (or customer user-role)
                                             :no-text true
                                             :class-name "user-role-segment"
                                             :as-elem :div}]]}]
     [sa/GridRow {:centered true}
      [sa/GridColumn {:width 6}
       [cost (take cur-round rounds) user-role]]]
     [info-group
      "exchange"
      "Der Warenfluss Deiner Firma."
      {}
      {:incoming [sa/GridColumn {:width 4} [incoming round-data]]
       :arrow-in [grid-arrow-column {:direction "right"}]
       :stock [sa/GridColumn {:width 6} [stock round-data]]
       :arrow-out [grid-arrow-column {:direction "right"}]
       :outgoing [sa/GridColumn {:width 4} [outgoing round-data]]}]]))

(defn game-view
  "Renders the game view for the current player."
  [user-role]
  (let [game (rf/subscribe [:game])]
    (fn [user-role]
      (let [{:as game-data
             :keys [:game/current-round :game/rounds :game/settings]} @game]
        (cond
          (not (spec/valid?
                :game/data game-data)) [msgs/render-message
                                        (msgs/invalid-game-data-msg
                                         (spec/explain-str :game/data game-data))]
          (or (>= current-round (count rounds))
              (neg? current-round)) (msgs/render-message
                                     (msgs/invalid-round-count current-round))
          :else
          [round-view rounds current-round user-role (:game/supply-chain settings)])))))

(defn overview-panel
  []
  (let [user (rf/subscribe [:user])
        game (rf/subscribe [:game])
        role (:user/role @user)
        events (rf/subscribe [:events])
        event-id (:event/id @user)]
    (ra/create-class
     {:component-did-mount #(rf/dispatch [:game/data-fetch])
      :reagent-render
      (fn []
        [sa/Container {:class-name "game-wrapper"}
         [game-view-header @game role]
         (if (get-in @events [event-id :event/started?])
           [game-view (:user/role @user)]
           [msgs/render-message (msgs/game-not-yet-started)])])})))
