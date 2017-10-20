(ns beer-game.handlers.game
  (:require [beer-game.store :as store]
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
  :round/commit
  [{:as msg}]
  (with-auth
    (fn [_ user-data]
      (->> (:?data msg)
           (game-logic/handle-commit (store/event-data (:event/id user-data)))
           (apply-update user-data)))))

(defmethod handle-game-msg :default
  [msg]
  {:type ::unhandled})

(defn apply-update
  "Applies the update returned by a game-logic handler
  to the store if valid and returns a message to returned to the user."
  [user-data update-map])
