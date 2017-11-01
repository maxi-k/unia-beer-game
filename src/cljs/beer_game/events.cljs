(ns beer-game.events
  (:require [re-frame.core :as rf]
            [secretary.core :as secretary]
            [beer-game.db :as db]
            [beer-game.util :as util]
            [beer-game.client :as client]
            [beer-game.components.messages :as messages]))


;; Register a fx-handler for sending websocket stuff
;; under the key ws
(rf/reg-fx
 :ws
 (fn [val]
   (client/send! val)))

;; fx-handler for an authorized websocket message
(rf/reg-fx
 :ws-auth
 (fn [[user-data [msg-type msg-data]]]
   (let [msg-data+auth (merge
                        (select-keys user-data [:user/id :user/realm :user/role
                                                :event/id])
                        msg-data)]
     (client/send! [msg-type msg-data+auth]))))

(rf/reg-fx
 :dispatch-multiple
 (fn [events]
   (doseq [event events]
     (rf/dispatch event))))

(rf/reg-fx
 :timed-dispatch
 (fn [[time msg]]
   (.setTimeout js/window
                (fn [] (rf/dispatch msg))
                time)))

(rf/reg-fx
 :goto
 (fn [url]
   (secretary/dispatch! url)))

(rf/reg-event-db
 :initialize-db
 (fn  [_ _]
   db/default-db))

(rf/reg-event-db
 :set-active-panel
 (fn [db [_ active-panel]]
   (assoc db :active-panel active-panel)))

(rf/reg-event-db
 :set-theme
 (fn [db [_]]
   (update-in db [:client :theme]
              util/toggle-value [:light :dark])))

(rf/reg-event-db
 :set-window-size
 (fn [db [_ width height]]
   (assoc-in db [:client :window] {:width width
                                   :height height})))

(rf/reg-event-db
 :test
 (fn [db [_ key data]]
   (assoc-in db [:test key] data)))

(rf/reg-event-fx
 :message/add
 (fn [{:keys [db]} [_ message]]
   (let [msg-id (str (random-uuid))
         other-params
         (if-let [timeout (:message/time message)]
           {:timed-dispatch [timeout [:message/remove msg-id]]}
           {})]
     (merge
      {:db (assoc-in db [:messages msg-id] message)}
      other-params))))

(rf/reg-event-db
 :message/remove
 (fn [db [_ id]]
   (update db :messages dissoc id)))

(rf/reg-event-fx
 :submission/invalid
 (fn [w [_]]
   {:dispatch [:message/add (messages/invalid-submission-msg)]}))

;;
;; Server Events
;;

(rf/reg-event-fx
 :server/echo-test
 (fn [_ [_ message]]
   {:ws [:testing/echo {:payload message}]}))

(rf/reg-event-fx
 :server/ping
 (fn [_]
   {:ws [client/ping-key]}))

;;
;; Authentication
;;

(rf/reg-event-fx
 :auth/unauthorized
 (fn [w [_ data]]
   (if (contains? data :auth/no-login)
     {:dispatch-multiple [[:message/add (messages/no-login-system-msg (str data))]
                          [:auth/logout false]]}
     {:dispatch [:message/add (messages/no-permission-system-msg (str data))]})))

(rf/reg-event-fx
 :auth/logout
 (fn [w [_ server-side?]]
   (let [db-map {:db (assoc (:db w) :user {:auth false})
                 :goto "/"}]
     (if server-side?
       (merge db-map {:ws [:auth/logout]})
       db-map))))

(rf/reg-event-db
 :auth/logout-success
 (fn [db [_]]
   (assoc-in db [:user :logout-success] true)))

(rf/reg-event-fx
 :auth/logout-forced
 (fn [w [_ data]]
   {:dispatch-multiple [[:auth/logout false]
                        [:message/add (messages/logout-forced-msg data)]]}))

(rf/reg-event-fx
 :auth/login
 (fn [_ [_ realm {:keys [:auth/key :event/id]}]]
   {:ws [:auth/login {:user/realm realm
                      :auth/key key
                      :event/id id}]}))

(rf/reg-event-db
 :auth/login-success
 (fn [db [_ data-map]]
   (update db :user merge data-map {:auth true
                                    :auth-failure false
                                    :logout-success true})))

(rf/reg-event-db
 :system/connection
 (fn [db [_ open?]]
   (assoc-in db [:client :connected] open?)))

(rf/reg-event-db
 :auth/login-invalid
 (fn [db [_ data]]
   (update db :user merge {:auth-failure true
                           :auth-failure-reason data})))


(rf/reg-event-fx
 :event/fetch
 (fn [{:keys [db]} [_ data]]
   {:ws-auth [(:user db) [:event/fetch data]]}))

(rf/reg-event-db
 :event/list
 (fn [db [_ data]]
   (assoc db :events data)))

(rf/reg-event-fx
 :event/create
 (fn [{:keys [db]} [_ data]]
   {:ws-auth [(:user db) [:event/create data]]}))

(rf/reg-event-db
 :event/created
 (fn [db [_ data]]
   (if (:created data)
     (update db :events assoc (:event/id data) data)
     db)))

(rf/reg-event-fx
 :event/destroy
 (fn [{:keys [db]} [_ {:keys [:event/id] :as data}]]
   {:ws-auth [(:user db) [:event/destroy {:event/id id}]]}))

(rf/reg-event-fx
 :event/destroyed
 (fn [w [_ {:keys [:event/id :destroyed] :as data}]]
   (if destroyed
     {:db (update (:db w) :events dissoc id)}
     {:dispatch [:message/add (messages/event-not-destroyed-msg (str data))]})))
