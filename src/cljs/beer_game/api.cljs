(ns beer-game.api
  (:require [re-frame.core :as rf]
            [beer-game.client :as client]))

(defmulti dispatch-server-event
  "Dispatches given event in the form of"
  :id)

(defmethod dispatch-server-event
  :default
  [params]
  (println "Unhandled Server Event Dispatched: " params))

(defmethod dispatch-server-event
  :testing/echo
  [{:keys [data]}]
  (rf/dispatch [:test :ws-echo data]))

(defn echo-test! [message]
  (client/send! [:testing/echo {:payload message}]))
