(ns beer-game.views.statistics
  "Statistics about a running or finished game using
  the plots defined in [[beer-game.components.plots]]."
  (:require [reagent.core :as ra]
            [re-frame.core :as rf]
            [reagent.core :as ra]
            [beer-game.config :as config]
            [beer-game.util :as util]
            [beer-game.components.plot :refer [draw-plot! make-plot-component]]
            [beer-game.components.event-selector :as event-selector]
            [beer-game.components.messages :as msgs]
            [soda-ash.core :as sa]))

(defn inverse-round-data
  "'Inverses' round-data by turning a vector of round data where each
  item contains information on each role for that round into a map
  which roles as keys and values as a vector of round information for
  that role."
  [rounds]
  (reduce
   (fn [coll round]
     (if-let [roles (:game/roles round)]
       (reduce
        (fn [coll [role role-data]]
          (update-in coll [role] (fnil conj []) role-data))
        coll
        roles)
       coll))
   {}
   rounds))

(defn per-round-plot!
  "A generic function drawing a plot that contains only data
  associated with one round."
  [x-data y-datas accessor layout]
  (fn [ref]
    (draw-plot! ref
                (map
                 (fn [[role rounds]]
                   {:x x-data
                    :y (map accessor rounds)
                    :name (config/user-role->title role)
                    :margin {:t 0}
                    :line {:color (config/user-role->color role)}})
                 y-datas)
                layout
                )))

(defn stock-statistic
  "Statistic for the stock of each round."
  [rounds round-data]
  (let [plot (make-plot-component
              "stock"
              (per-round-plot! rounds round-data :round/stock
                               {:title "Gesamter Lagerbestand"
                                :xaxis {:title "Rundenzahl"}
                                :yaxis {:title "Lagerbestand in Stück"}}))]
    [:div
     [:h2 "Lagerbestand"]
     [plot]]))

(defn cost-per-round-statistic
  "Statistic for the cost of each round (not the overall accumulated cost!)"
  [rounds round-data]
  (let [plot (make-plot-component
              "cost-per-round"
              (per-round-plot! rounds round-data :round/cost
                               {:title "Kosten pro Runde"
                                :xaxis {:title "Rundenzahl"}
                                :yaxis {:title "Kosten in Geldeinheiten"}}))]
    [:div
     [:h2 "Kosten pro Runde"]
     [plot]]))

(defn incoming-statistic
  "Statistic for the incoming product per round."
  [rounds round-data]
  (let [plot (make-plot-component
              "incoming-per-round"
              (per-round-plot! rounds round-data :round/incoming
                               {:title "Wareneingang"
                                :xaxis {:title "Rundenzahl"}
                                :yaxis {:title "Wareneingang in Stück"}}))]
    [:div
     [:h2 "Wareneingang"]
     [plot]]))

(defn demand-statistic
  "Statistic for the demand per round."
  [rounds round-data]
  (let [plot (make-plot-component
              "outgoing-per-round"
              (per-round-plot! rounds round-data :round/demand
                               {:title "Anfragen"
                                :xaxis {:title "Rundenzahl"}
                                :yaxis {:title "Anfrage in Stück"}}))]
    [:div
     [:h2 "Anfragen"]
     [plot]]))

(defn debt-per-round-statistic
  "Statistic for the debt per round."
  [rounds round-data]
  (let [plot (make-plot-component
              "debt-per-round"
              (per-round-plot! rounds round-data :round/debt
                               {:title "Ausstehende Einheiten"
                                :xaxis {:title "Rundenzahl"}
                                :yaxis {:title "Ausstehend in Stück"}}))]
    [:div
     [:h2 "Ausstehend"]
     [plot]]))

(defn statistics
  "Displays all statistics. Also displays messages for when
  the selected event does not exist or has not been started."
  [event-data]
  (cond
    (nil? event-data)
    [msgs/render-message (msgs/no-such-event)]
    ;; -----
    (not (:event/started? event-data))
    [msgs/render-message (msgs/game-not-yet-started)]
    ;; ------
    :else
    (let [game-data (:game/data event-data)
          cur-round (inc (get game-data :game/current-round))
          rounds (range 1 cur-round)
          round-data (inverse-round-data (take cur-round (get game-data :game/rounds)))]
      [:div
       [stock-statistic rounds round-data]
       [cost-per-round-statistic rounds round-data]
       [debt-per-round-statistic rounds round-data]
       [incoming-statistic rounds round-data]
       [demand-statistic rounds round-data]
       ])))

(defn statistics-panel
  "The Component which wraps all statistics and is connected
  to the client-side re-frame store."
  []
  (let [user (rf/subscribe [:user])
        events (rf/subscribe [:events])
        single-event? (util/single-event? (:event/id @user))
        selected-event (if single-event?
                         (ra/cursor user [:event/id])
                         (rf/subscribe [:selected-event]))]
    (fn []
      [:section
       [:h1 "Statistiken"]
       [event-selector/connected-event-selector]
       [sa/Divider]
       (if (nil? @selected-event)
         [msgs/select-event-msg]
         (let [event (get @events @selected-event)]
           [statistics event]))])))
