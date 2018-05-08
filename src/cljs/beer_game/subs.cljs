(ns beer-game.subs
  "Contains Subscriptions to the client-side store for the
  user-view to use, which provide a part of the data stored.
  As these are not actual clojure definitions, they will
  not show up in the html-documentation. They are, however,
  mostly self-explanatory in the code."
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
 :user/role
 :<- [:user]
 (fn [user]
   (:user/role user)))

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

(rf/reg-sub
 :game/rounds
 :<- [:game]
 :game/rounds)

(rf/reg-sub
 :game/current-round
 :<- [:game]
 :game/current-round)

(rf/reg-sub
 :game/settings
 :<- [:game]
 :game/settings)

(rf/reg-sub
 :game/supply-chain
 :<- [:game/settings]
 :game/supply-chain)

(rf/reg-sub
 :game/current-round-data
 :<- [:game/rounds]
 :<- [:game/current-round]
 (fn [rounds current-round]
   (get rounds current-round)))

(rf/reg-sub
 :game/game-state
 (fn [db]
   (get db :game-state)))

(rf/reg-sub
 :game/acknowledgements
 (fn [db]
   (get-in db [:game-state :acknowledgements])))

(rf/reg-sub
 :game/round-acknowledgement
 :<- [:game/acknowledgements]
 :<- [:game/current-round]
 (fn [acks cur-round]
   (get acks cur-round false)))

(rf/reg-sub
 :game/round-incoming->stock
 :<- [:game/game-state]
 :<- [:game/current-round]
 (fn [[game-state cur-round]]
   (get-in game-state [:incoming->stock cur-round] false)))

(rf/reg-sub
 :game/round-stock->outgoing
 :<- [:game/game-state]
 :<- [:game/current-round]
 (fn [[game-state cur-round]]
   (get-in game-state [:stock->outgoing cur-round] false)))
