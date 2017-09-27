(ns beer-game.api
  (:require [re-frame.core :as rf]))

(defn dispatch-server-event
  "Dispatches a server event using re-frame in the form of
  {:id event-id :data event-data}"
  [{:keys [id data] :as params}]
  (condp = id
    :auth/login-invalid  (rf/dispatch [:auth/login-invalid data])
    :auth/login-success  (rf/dispatch [:auth/login-success data])
    :auth/logout-success (rf/dispatch [:auth/logout-success])
    :auth/unauthorized   (rf/dispatch [:auth/unauthorized])
    :system/connection   (rf/dispatch [:system/connection data])
    :testing/echo        (rf/dispatch [:test :ws-echo data])
    :chsk/ws-ping        (rf/dispatch [:server/ping])
    (println "Unhandled Server Event Dispatched: " params)))

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
