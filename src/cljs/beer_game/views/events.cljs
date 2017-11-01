(ns beer-game.views.events
  (:require [re-frame.core :as rf]
            [reagent.core :as ra]
            [soda-ash.core :as sa]
            [beer-game.client-util :as cutil]
            [beer-game.config :as config]
            [beer-game.components.modals :as modals]
            [beer-game.components.inputs :as inputs]
            [beer-game.components.tables :as tables]
            [beer-game.spec.event]
            [beer-game.spec.game]))

(defn- event-role-list
  ([user-list] (event-role-list {} user-list))
  ([options user-list]
   [sa/ListSA (merge {:bulleted true} options)
    (for [user user-list
          :let [title (-> user :user/role config/user-role->title)]]
      [sa/ListItem {:key (:user/role user)} title])]))

(defn- create-event-form
  [modal-close-fn]
  (let [form-values (ra/atom {:event/id (cutil/gen-event-id)
                              :event/name ""
                              :game/data {:game/settings {:round-amount 20}}})
        update-form (fn [k v]
                      (if (vector? k)
                        (swap! form-values assoc-in k (.-value v))
                        (swap! form-values assoc k (.-value v))))
        submit-form (ra/atom (fn [e]
                               (rf/dispatch [:event/create @form-values])
                               (modal-close-fn)))
        input-options [{:key :event-id
                        :label "Event ID"
                        :placeholder "Event-ID"
                        :spec :event/id
                        :invalid-msg "Bitte eine Event-ID ohne Leerzeichen eingeben."
                        :value-fn #(:event/id @form-values)
                        :on-change #(update-form :event/id %2)}
                       {:key :event-name
                        :label "Event Name"
                        :placeholder "Ausgeschriebener Name des Events"
                        :spec :event/name
                        :invalid-msg "Bitte einen beliebigen, nicht-leeren Namen für das Event eingeben."
                        :value-fn #(:event/name @form-values)
                        :on-change #(update-form :event/name %2)}
                       {:key :round-amount
                        :label "Rundenzahl"
                        :transform js/parseInt
                        :spec :beer-game.spec.game/round-amount
                        :invalid-msg "Bitte eine positive, ganze Zahl eingeben."
                        :value-fn #(get-in @form-values [:game/data :game/settings :round-amount])
                        :on-change #(update-form [:game/data :game/settings :round-amount] %2)}]
        input-elements (doall (map inputs/make-validated-input input-options))]
    (fn []
      [inputs/validated-form
       {:validated-inputs input-elements
        :submit-atom submit-form}
       (for [elem input-elements]
         ^{:key (:key elem)}
         [inputs/validated-input elem])
       [sa/FormButton {:key :btn
                       :primary true
                       :class-name :clearfloat
                       :floated :right
                       :content "Erstellen"
                       :onClick #(@submit-form)}]])))

(defn- create-event-modal
  [trigger]
  [modals/generic-modal trigger
   "Neues Event erstellen"
   (fn [modal-state]
     [create-event-form #(reset! modal-state false)])
   {}])

(defn- show-event-modal
  [trigger event-data]
  [modals/generic-modal trigger
   (:event/name event-data)
   (fn [modal-state]
     [:div
      [tables/definition-table
       {"Event ID" (:event/id event-data)
        "Event Name" (:event/name event-data)
        "Rundenzahl" (get-in event-data [:game/data :game/settings :round-amount])
        "Spieler" [event-role-list {:bulleted false} (:user/list event-data)]}]
      [sa/Button {:on-click #(reset! modal-state false)
                  :floated :right}
       "Schließen"]
      [:div.clearfloat]])
   {}])

(defn- event-menu
  []
  (let [theme (rf/subscribe [:client/theme])]
    (fn []
      [sa/Menu {:class-name "themeable"
                :inverted (= :dark @theme)
                :size :huge}
       [sa/MenuItem {:header true
                     :icon "users"
                     :content "Events"}]
       [create-event-modal
        [sa/MenuItem {:content "Event erstellen"
                      :icon "plus"}]]
       [sa/MenuItem {:position :right
                     :on-click #(rf/dispatch [:event/fetch])
                     :icon "refresh"}]])))

(defn- event-actions
  "Actions buttons for a single event (RUD)."
  [event]
  (let [modal-state (ra/atom {:delete false})]
    (fn [event]
      [:div.clearfloat
       [sa/ButtonGroup {:floated :right}
        [show-event-modal
         [sa/Button {} "Anzeigen"]
         event]
        [sa/Button {:negative true
                    :on-click #(swap! modal-state assoc :delete true)}
         "Löschen"]
        [sa/Confirm {:open (:delete @modal-state)
                     :cancel-button "Abbrechen"
                     :header "Event löschen"
                     :content "Sicher? Alle Spieler-Sessions in diesem Event werden beendet."
                     :on-confirm #(do (rf/dispatch [:event/destroy event])
                                      (swap! modal-state assoc :delete false))
                     :on-cancel #(swap! modal-state :delete false)}]]])))

(defn- event-list
  []
  (let [events (rf/subscribe [:events])
        theme (rf/subscribe [:client/theme])]
    (fn []
      [sa/Table {:class-name "themeable"
                 :inverted (= :dark @theme)
                 :selectable true
                 :size "large"}
       [sa/TableHeader
        [sa/TableRow
         [sa/TableHeaderCell "Event Name"]
         [sa/TableHeaderCell "Event ID"]
         [sa/TableHeaderCell "Spieler"]
         [sa/TableHeaderCell "Aktionen"]]]
       [sa/TableBody
        (for [[_ event] @events
              :let [{:keys [:event/id :event/name]
                     user-list :user/list} event]]
          [sa/TableRow {:key id}
           [sa/TableCell name]
           [sa/TableCell id]
           [sa/Popup {:hoverable true
                      :trigger (ra/as-element [sa/TableCell (count user-list)])}
            [sa/PopupHeader "Belegte Rollen"]
            [sa/PopupContent
             [event-role-list user-list]]]
           [sa/TableCell [event-actions event]]])]])))

(defn events-panel
  "Renders the panel for the events view."
  []
  (let [user (rf/subscribe [:name])]
    (ra/create-class
     {:display-name :events-panel
      :component-will-mount #(rf/dispatch [:event/fetch])
      :reagent-render
      (fn []
        [:div
         [event-menu]
         [event-list]])})))
