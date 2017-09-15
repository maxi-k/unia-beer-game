(ns beer-game.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 :name
 (fn [db]
   (:name db)))

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
 :test
 (fn [db _]
   (get-in db [:test])))
