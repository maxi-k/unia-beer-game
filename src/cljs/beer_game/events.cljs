(ns beer-game.events
  (:require [re-frame.core :as rf]
            [beer-game.db :as db]
            [beer-game.util :as util]
            [beer-game.client :as client]))


;; Register a fx-handler for sending websocket stuff
;; under the key ws
(rf/reg-fx
 :ws
 (fn [val]
   (client/send! val)))

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
 :auth/logout
 (fn [w [_ server-side?]]
   (let [db-map {:db (assoc (:db w) :user {:auth false})}]
     (if server-side?
       (merge db-map {:ws [:auth/logout]})
       db-map))))

(rf/reg-event-db
 :auth/logout-success
 (fn [db [_]]
   (assoc-in db [:user :logout-success] true)))

(rf/reg-event-fx
 :auth/login
 (fn [_ [_ realm key]]
   {:ws [:auth/login {:realm realm :key key}]}))

(rf/reg-event-db
 :auth/login-success
 (fn [db [_ {:keys [uid uuid realm]}]]
   (update db :user merge {:auth true
                           :auth-failure false
                           :logout-success true
                           :uid uid
                           :uuid uuid
                           :realm realm})))

(rf/reg-event-db
 :auth/login-invalid
 (fn [db [_ data]]
   (update db :user merge {:auth-failure true
                           :auth-failure-reason data})))
