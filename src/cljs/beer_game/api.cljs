(ns beer-game.api
  (:require [re-frame.core :as rf]
            [beer-game.components.messages :as msgs]
            [beer-game.config :as config]))

(defn dispatch-server-event
  "Dispatches a server event using re-frame in the form of
  {:id event-id :data event-data}"
  [{:keys [id data] :as params}]
  (condp = id
    :auth/login-invalid  (rf/dispatch [:auth/login-invalid data])
    :auth/login-success  (rf/dispatch [:auth/login-success data])
    :auth/logout-success (rf/dispatch [:auth/logout-success])
    :auth/logout-forced  (rf/dispatch [:auth/logout-forced data])
    :auth/unauthorized   (rf/dispatch [:auth/unauthorized data])
    :event/created       (rf/dispatch [:event/created data])
    :event/destroyed     (rf/dispatch [:event/destroyed data])
    :event/list          (rf/dispatch [:event/list data])
    :game/data           (rf/dispatch [:game/data data])
    :system/connection   (rf/dispatch [:system/connection data])
    :testing/echo        (rf/dispatch [:test :ws-echo data])
    :chsk/ws-ping        (rf/dispatch [:server/ping])
    (if config/development?
      (rf/dispatch [:message/add (msgs/debug-msg "Unhandled Server Event: " params)])
      (.log js/console "Unhandled Server Event Dispatched: " params))))

(defn echo-test!
  "Sends an echo message to the websocket for debugging purposes."
  [message]
  (rf/dispatch [:server/echo-test message]))

(defn logout!
  "Logs out the current user."
  ([server-side?]
   (rf/dispatch [:auth/logout server-side?]))
  ([] (logout! false)))

(defn nil-uid?
  "Returns true if given uid value is considered nil."
  [uid]
  (or (nil? uid)
      (= uid :taoensso.sente/nil-uid)))
