(ns beer-game.views.events
  (:require [re-frame.core :as rf]
            [reagent.core :as ra]
            [soda-ash.core :as sa]
            [beer-game.client-util :as cutil]
            [beer-game.config :as config]))

(defn- create-event-form
  [modal-close-fn]
  (let [form-values (ra/atom {:event/id (cutil/gen-event-id)
                              :event/name ""})
        update-form (fn [k v] (swap! form-values assoc k (.-value v)))
        submit-form (fn [e]
                      (rf/dispatch [:event/create @form-values])
                      (modal-close-fn))]
    (fn []
      [sa/Form
       [sa/FormInput {:label "Event ID"
                      :value (:event/id @form-values)
                      :on-change #(update-form :event/id %2)}]
       [sa/FormInput {:label "Event Name"
                      :value (:event/name @form-values)
                      :placeholder "Ausgeschriebener Name des Events"
                      :on-change #(update-form :event/name %2)}]
       [sa/FormButton {:primary true
                       :class-name :clearfloat
                       :floated :right
                       :content "Erstellen"
                       :onClick submit-form}]])))

(defn- create-event-modal
  [trigger]
  (let [modal-state (ra/atom false)
        trigger-el (->> trigger
                        (cutil/with-options-raw
                          {:onClick #(reset! modal-state true)})
                        ra/as-element)]
    (fn []
      [sa/Modal {:trigger trigger-el
                 :dimmer :blurring
                 :open @modal-state}
       [sa/Icon {:name "close"
                 :onClick #(reset! modal-state false)}]
       [sa/ModalHeader
        "Neues Event erstellen"]
       [sa/ModalContent
        [create-event-form #(reset! modal-state false)]]])))

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
         [sa/TableHeaderCell "Spieler"]]]
       [sa/TableBody
        (for [[_ {:keys [:event/id :event/name]
                  user-list :user/list}] @events]
          [sa/TableRow {:key id}
           [sa/TableCell name]
           [sa/TableCell id]
           [sa/Popup {:hoverable true
                      :trigger (ra/as-element [sa/TableCell (count user-list)])}
            [sa/PopupHeader "Belegte Rollen"]
            [sa/PopupContent
             [sa/ListSA {:bulleted true}
              (for [user user-list
                    :let [title (-> user :user/role config/user-role->title)]]
                [sa/ListItem {:key (:user/role user)} title])]
             ]]])]])))

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
