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
  :commit-round
  [{:as msg}]
  (with-auth msg
    (fn [_ user-data]
      {:type :reply
       :message
       (->> (:?data msg)
            #((assoc % :user/role (:user/role user-data)))
            (game-logic/handle-commit (store/game-data (:event/id user-data)))
            (apply-update user-data))})))

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
         :message [:game/data (:game/data (store/events event-id))]}
        (if (store/single-event? event-id)
          {:type :reply
           :message
           (if-let [event (:game/data (store/events event-id))]
             [:game/data
              (update event :game/rounds util/filter-round-data user-role)]
             [:game/data {:invalid :event/id}])}
          {:type :reply
           :message [:auth/unauthorized {:event/id event-id}]})))))

(defmethod handle-game-msg :default
  [msg]
  {:type ::unhandled})

(defn apply-update
  "Applies the update returned by a game-logic handler
  to the store if valid and returns a message to returned to the user."
  [user-data update-map])
