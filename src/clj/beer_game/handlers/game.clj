(ns beer-game.handlers.game
  (:require [beer-game.store :as store]
            [beer-game.config :as config]
            [beer-game.util :as util]
            [beer-game.logic.game :as game-logic]))

(defn- with-auth
  [msg reply-fn]
  (let [{:as user-data
         realm :user/realm
         event-id :event/id} (store/message->user-data msg)]
    (cond
      (nil? user-data) {:type :reply
                        :message [:auth/unauthorized {:user/realm realm}]}
      (nil? event-id)  {:type :reply
                        :message [:auth/unauthorized {:event/id event-id}]}
      :else (if (fn? reply-fn)
              (reply-fn msg user-data)
              reply-fn))))

(declare apply-update)

(defmulti handle-game-msg
  "Dispatches on all game events."
  :internal-id)

(defmethod handle-game-msg
  :data-fetch
  [msg]
  (with-auth msg
    (fn [_ {:as user-data
           user-realm :user/realm
           user-role :user/role
           event-id :event/id}]
      (if (= user-realm config/leader-realm)
        {:type :reply
         :message [:game/data (if (util/single-event? event-id)
                                (:game/data (store/events event-id))
                                (store/events event-id))]}
        (if (util/single-event? event-id)
          {:type :reply
           :message
           (let [event (store/events event-id)
                 game-data (:game/data event)]
             (if game-data
               [:game/data
                (-> game-data
                    (update :game/rounds util/filter-round-data user-role)
                    (assoc :event/id (:event/id event)))]
               [:game/data {:invalid :event/id}]))}
          {:type :reply
           :message [:auth/unauthorized {:event/id event-id}]})))))

(defmethod handle-game-msg
  :round-commit
  [{:as msg}]
  (with-auth msg
    (fn [_ user-data]
      (->> (:?data msg)
           (#(if (int? (:round/order %))
               %
               (update % :round/order read-string)))
           (#(assoc % :user/role (:user/role user-data)))
           (game-logic/handle-commit (store/game-data (:event/id user-data)))
           (apply-update user-data)))))

(defmethod handle-game-msg
  :round-ready
  [{:as msg}]
  (with-auth msg
    (fn [_ user-data]
      (->> (:?data msg)
           (#(assoc % :user/role (:user/role user-data)))
           (game-logic/handle-ready (store/game-data (:event/id user-data)))
           (apply-update user-data)))))

(defmethod handle-game-msg :default
  [msg]
  {:type ::unhandled})

(defn apply-update
  "Applies the update returned by a game-logic handler
  to the store if valid and returns an internal-message to be
  sent to the user(s)."
  [{:as user-data
    user-role :user/role
    event-id :event/id} update-map]
  (cond
    (empty? update-map)
    {:type :reply
     :message [:game/data-update {:game/updated? false
                                  :update/reason {:unknown true
                                                  :update-data update-map}}]}
    ;; -----
    (not (:update/valid? update-map))
    {:type :reply
     :message [:game/data-update {:game/updated? false
                                  :update/reason (:update/reason update-map)}]}
    ;; -----
    :else
    (let [new-round? (get-in [:update/diff :game/current-round] nil)
          game-update (-> (store/update-game! event-id (:game/data update-map))
                          (assoc :event/id event-id))]
      (if (and (:game/updated? game-update)
               new-round?)
        {:type :broadcast
         :uids (store/event->clients event-id)
         :message (fn [client-id]
                    (let [user (store/client-id->user-data client-id)
                          role (:user/role user)]
                      (if (= config/leader-realm (:user/realm user))
                        [:game/data-update game-update]
                        [:game/data-update
                         (update game-update :game/rounds
                                 util/filter-round-data role)])))}
        {:type :reply
         :message [:game/data-update (update :game/rounds
                                             util/filter-round-data user-role)]}))))
