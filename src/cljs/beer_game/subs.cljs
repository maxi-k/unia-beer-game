(ns beer-game.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as rf]
            [beer-game.util :as util]))

(rf/reg-sub
 :name
 (fn [db]
   (:name db)))

(rf/reg-sub
 :user
 (fn [db]
   (:user db)))

(rf/reg-sub
 :active-panel
 (fn [db _]
   (:active-panel db)))

(rf/reg-sub
 :client
 (fn [db _]
   (:client db)))

(rf/reg-sub
 :client/window
 :<- [:client]
 (fn [client]
   (:window client)))

(rf/reg-sub
 :client/theme
 :<- [:client]
 (fn [client]
   (or (:theme client) :light)))

(rf/reg-sub
 :client/sidebar
 :<- [:client]
 (fn [client]
   (:sidebar client)))

(rf/reg-sub
 :client/connected
 :<- [:client]
 (fn [client]
   (:connected client)))

(rf/reg-sub
 :messages
 (fn [db]
   (:messages db)))

(rf/reg-sub
 :test
 (fn [db]
   (get-in db [:test])))

(rf/reg-sub
 :events
 (fn [db]
   (get-in db [:events])))

(rf/reg-sub
 :event
 :<- [:events]
 (fn [events event-id]
   (get events event-id)))

(rf/reg-sub
 :selected-event
 (fn [db]
   (get db :selected-event)))

(rf/reg-sub
 :game
 (fn [db]
   (let [event-id (get-in db [:user :event/id])]
     (if (util/single-event? event-id)
       (get-in db [:events event-id :game/data])
       (map (fn [[k v]] (:game/data v))
            (get-in db [:events]))))))
