(ns beer-game.views.overview
  "The main overview for the game information for running games."
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
            [:div.role-title.arrow {:key (str "arrow-" idx)}
             [sa/Icon {:name "arrow right"
                       :size "large"}]]))
         (doall))
        [:div.clearfloat]]
       [sa/Divider {:class-name "game-view-header-subtitle"
                    :horizontal true}
        (str "Runde " (:game/current-round game))]])))

(defn game-area
  "A single block of information about the game."
  ([options area title child]
   (let [area-str (if area (name area) "")]
     [sa/SegmentGroup (merge {:class-name (str "game-area " area-str)}
                             options)
      [sa/Segment {:class-name "title"}
       [:h3 title]]
      [sa/Segment {:class-name "content-wrapper"}
       [:div.content {:style (when area
                               {:background-image
                                (str "url(" config/game-area-path "/" area-str ".svg" )})}
        child]]])))

(defn unit-text
  "A component that renders the text for a unit,
  to give the numeric values context."
  [text]
  [:p.unit-text text])

(defn outgoing
  "The part of the view that represents the outgoing items
  for the current-player for the previous round."
  [round-data cur-round]
  (let [transferred? (rf/subscribe [:game/round-stock->outgoing])]
    (fn [{:as round-data :keys [:round/outgoing]}
        cur-round]
      [game-area {} :outgoing
       "Warenausgang"
       [:div.message-data
        [:span.main-value
         (if @transferred?
           (or outgoing 0)
           "--")]
        [unit-text "Einheiten"]
        [sa/Button
         {:disabled @transferred?
          :primary true
          :on-click #(do (rf/dispatch [:game/acknowledge-round cur-round])
                         (rf/dispatch [:game/round-stock->outgoing cur-round]))}
         "Aus dem Lager holen"]]])))

(defn incoming
  "The part of the view that represents the production / incoming items
  for the current player."
  [cur-round]
  (let [transferred? (rf/subscribe [:game/round-incoming->stock])]
    (fn [{:as round-data :keys [:round/incoming]} cur-round]
      (.log js/console @transferred?)
      [game-area {} :incoming
       "Wareneingang"
       [:div.message-data
        [:span.main-value
         (if @transferred?
           "--"
           (or incoming 0))]
        [unit-text "Einheiten"]
        [sa/Button
         {:disabled @transferred?
          :primary true
          :on-click #(rf/dispatch [:game/round-incoming->stock cur-round])}
         "In das Lager stellen"]]])))

(defn mail
  "The part of the view that represents the mailbox of the current player."
  [round-data]
  [game-area {} :mail
   "Angefragt"
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
  (let [stock->outgoing? (rf/subscribe [:game/round-stock->outgoing])
        incoming->stock? (rf/subscribe [:game/round-incoming->stock])]
    (fn [round-data]
      [game-area {} :stock
       "Lager"
       [:div.message-data
        [:span.main-value
         (cond-> (:round/stock round-data)
           @incoming->stock? (+ (:round/incoming round-data))
           @stock->outgoing? (- (game-logic/calc-deliverable round-data)))]
        [unit-text "Einheiten"]]])))

(defn debt
  "The part of the view that represents the overall debt of the current player
  for the rounds given."
  [round-data]
  (let [overall-debt (or (:round/debt round-data) 0)]
    [game-area {} :debt
     "Ausstehend"
     [:div.message-data {:style {:color (if (= overall-debt 0) "#21BA45" "#DB2828")}}
      [:span.main-value
       overall-debt]
      [unit-text "Einheiten"]]]))

(defn cost
  "The part of the view that represents the overall cost
  for the rounds given. "
  [rounds user-role]
  [game-area {} :cost
   "Gesamtkosten"
   [:div.message-data
    [:span.main-value
     (game-logic/overall-cost rounds user-role)]
    [unit-text "Dollar"]]])

(defn commit-round-btn
  "Button Component for commiting the round (ordering)."
  [submit-fn commited?]
  [sa/Button {:key :btn
              :primary true
              :disabled commited?
              :on-click submit-fn
              :content (if commited? "Bestellt" "Bestellen")}])

(defn order
  "The part of the view that represents the request
  this player has for the guy above him in the supply chain."
  [round-data]
  (let [form-data (atom {:round/order 0})
        input-data (inputs/make-validated-input
                    {:class-name "order-input"
                     :spec :round/order
                     :placeholder "Bestellung"
                     :transform js/parseInt
                     :on-change #(swap! form-data assoc :round/order (.-value %2))
                     :value-fn #(:round/order @form-data)
                     :invalid-msg "Bitte eine ganze Zahl eingeben."})
        submit-fn (atom #(rf/dispatch [:game/round-commit @form-data]))]
    (fn [round-data]
      [game-area {} :request
       "Anfrage"
       [:div.message-data
        [inputs/validated-form {:as sa/Form
                                :style {:display :block}
                                :validated-inputs [input-data]
                                :submit-atom submit-fn}
         ^{:key :input} [inputs/validated-input input-data]
         ^{:key :units} [unit-text "Einheiten"]
         ^{:key :button} [commit-round-btn
                          @submit-fn
                          (:round/commited? round-data)]]]])))

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
                      :class-name "info-group"}
                     options)
   [info-group-icon icon-name description]
   (for [[key elem] children]
     (cutil/with-options-raw {:key key} elem))])

(defn grid-arrow-column
  "Renders a grid cloumn designated to an arrow."
  [{:as opts
    :keys [direction width rotation]
    :or {direction "right" width 1}}]
  [sa/GridColumn (merge {:width width
                         :class-name "game-area-divider"
                         :style (when rotation {:transform (str "rotate(" rotation ")")})}
                        (dissoc opts :direction :rotation))
   [sa/Icon {:name (str "arrow " direction)}]])

(defn grid-curved-arrow-column
  "A curved arrow indicating the flow of products/information.
  Is passed the rotation as an argument."
  [{:as opts
    :keys [rotation width]
    :or {rotation 0
         width 2}}]
  [sa/GridColumn (merge
                  {:width width
                   :class-name (str "game-area-curved-arrow " rotation)}
                  (dissoc opts :rotation))
   [sa/Image {:src (str config/icon-path "curved-arrow.svg")}]])

(defn supply-chain-column-box
  "An infobox for the supply-chain element given by the user-role."
  [{:as options
    :keys [width user-role title]
    :or {width 3}}]
  [sa/GridColumn {:width width}
   [game-area {} nil ;; No background icon
    (or title (config/user-role->title user-role))
    [user-role-image
     {:user-role user-role
      :no-text true
      :class-name "user-role-segment"
      :as-elem :div}]]])

(defn next-round-button
  "Button for signaling that the player is ready for the next round."
  [cur-round ready? commited?]
  (let [stock->outgoing? (rf/subscribe [:game/round-stock->outgoing])
        incoming->stock? (rf/subscribe [:game/round-incoming->stock])]
    (fn [cur-round ready? commited?]
      [sa/Button {:on-click #(rf/dispatch [:game/round-ready {:target-round cur-round}])
                  :disabled (or (not commited?)
                                (not @stock->outgoing?)
                                (not @incoming->stock?)
                                ready?)
                  :primary true}
       (cond
         (not commited?) "Bitte zuerst bestellen"
         (not @incoming->stock?) "Bitte zuerst Wareneingang bestätigen"
         (not @stock->outgoing?) "Bitte zuerst Warenausgang bestätigen"
         ready? "Warten auf andere Spieler..."
         :else "Nächste Runde")])))

(defn cost-multiplier-arrow
  "Arrow indicating how much the stock/debt contributes to the cost of
  a single round."
  [{:as options
    :keys [direction]}
   content]
  (let [wrapper-opts (merge {:class-name (str "cost-multiplier-arrow "
                                              (:class-name options))}
                            (dissoc options :direction :class-name))
        icon [sa/Icon {:name (str "arrow " (name direction))
                       :class-name "arrow-icon"
                       :size "huge"}]
        string [:p.multiplier-string
                [sa/Icon {:name "x" :fitted true :size "large"}]
                content " Geldeinheiten/Stück"]]
    (if (= (keyword direction) :down)
      [:div wrapper-opts string icon]
      [:div wrapper-opts icon string])))

(defn round-view
  "Renders the game view for the current round."
  [rounds cur-round user-role {:as settings :keys [:game/supply-chain
                                                   stock-cost-factor
                                                   debt-cost-factor]}]
  (let [round-data (get-in (nth rounds cur-round) [:game/roles user-role])
        [supplier customer] (game-logic/roles-around user-role supply-chain)]
    [sa/Grid {:class-name "game-view-grid"
              :vertical-align "middle"
              :columns 5}
     [info-group
      "inbox"
      "Der Informationsfluss Deiner Firma."
      {}
      [[:arrow-out [grid-curved-arrow-column {:rotation "down-left"}]]
       [:outgoing [sa/GridColumn {:width 4} [order round-data]]]
       [:debt [sa/GridColumn {:width 4} [debt round-data]]]
       [:demand [sa/GridColumn {:width 4} [mail round-data]]]
       [:arrow-in [grid-curved-arrow-column {:rotation "up-left"}]]]]
     [sa/GridRow {:centered true}
      [supply-chain-column-box {:user-role (or supplier user-role)
                                :title (when (= user-role (first supply-chain))
                                         "Produktion")}]
      [sa/GridColumn {:width 2}]
      [sa/GridColumn {:width 6}
       [cost-multiplier-arrow {:direction :down} debt-cost-factor]
       [cost (take cur-round rounds) user-role]
       [cost-multiplier-arrow {:direction :up} stock-cost-factor]]
      [sa/GridColumn {:width 2}]
      [supply-chain-column-box {:user-role (or customer user-role)
                                :title (when (= user-role (last supply-chain))
                                         "Konsum")}]]
     [info-group
      "exchange"
      "Der Warenfluss Deiner Firma."
      {}
      [[:arrow-in1 [grid-curved-arrow-column {:rotation "down-right"}]]
       [:incoming [sa/GridColumn {:width 3} [incoming round-data cur-round]]]
       [:arrow-in2 [grid-arrow-column {:direction "right"}]]
       [:stock [sa/GridColumn {:width 4} [stock round-data]]]
       [:arrow-out1 [grid-arrow-column {:direction "right"}]]
       [:outgoing [sa/GridColumn {:width 3} [outgoing round-data cur-round]]]
       [:arrow-out2 [grid-curved-arrow-column {:rotation "up-right"}]]]]
     [sa/GridRow {:centered true}
      [sa/GridColumn
       [next-round-button
        cur-round
        (get round-data :round/ready?)
        (get round-data :round/commited?)]]]]))

(defn game-view
  "Renders the game view for the current player."
  [user-role]
  (let [game (rf/subscribe [:game])]
    (fn [user-role]
      (let [{:as game-data
             :keys [:game/current-round :game/rounds :game/settings]} @game]
        (cond
          (or (nil? current-round) (empty? rounds)
              (>= current-round (count rounds))
              (neg? current-round)) (msgs/render-message
                                     (msgs/invalid-round-count current-round (count rounds)))
          (not (spec/valid?
                :game/data game-data)) [msgs/render-message
                                        (msgs/invalid-game-data-msg
                                         (spec/explain-str :game/data game-data))]
          :else
          [round-view rounds current-round user-role settings])))))

(defn overview-panel
  "The panel that wraps all of the game-view and is connected to the store."
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
