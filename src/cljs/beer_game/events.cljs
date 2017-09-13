(ns beer-game.events
  (:require [re-frame.core :as rf]
            [beer-game.db :as db]
            [beer-game.util :as util]))

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
 :do-ajax-test
 (fn [db [_ key response]]
   (assoc-in db [:test :ajax key] response)))
