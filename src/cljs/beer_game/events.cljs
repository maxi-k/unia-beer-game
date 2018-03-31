(ns beer-game.events
  "Registeres all used event with re-frame to be used
  in the client-side dispatcher. Also defines some higher-order
  fx-handlers as utility, like `timed-dispatch`,  `dispatch-multiple`
  and some websocket-specific actions like `ws` and `ws-auth`.

  As these handlers are not actually clojure definitions,
  they will not show up in the html documentation. Please refer to the
  code-comments for documentation."
  (:require [re-frame.core :as rf]
            [secretary.core :as secretary]
            [beer-game.db :as db]
            [beer-game.util :as util]
            [beer-game.client :as client]
            [beer-game.components.messages :as messages]
            [beer-game.components.messages :as msgs]))


;; Register a fx-handler for sending websocket stuff
;; under the key ws
(rf/reg-fx
 :ws
 (fn [val]
   (client/send! val)))

;; fx-handler for an authorized websocket message
(rf/reg-fx
 :ws-auth
 (fn [[user-data [msg-type msg-data]]]
   (let [msg-data+auth (merge
                        (select-keys user-data [:user/id :user/realm :user/role
                                                :event/id])
                        msg-data)]
     (client/send! [msg-type msg-data+auth]))))

;; Passed a list of events to dispatch, dispatches them
;; in the order of the list.
(rf/reg-fx
 :dispatch-multiple
 (fn [events]
   (doseq [event events]
     (rf/dispatch event))))

;; Passed a time and a message, dispatches the given
;; message after the given time.
(rf/reg-fx
 :timed-dispatch
 (fn [[time msg]]
   (.setTimeout js/window
                (fn [] (rf/dispatch msg))
                time)))

;; Passed a url, sets the client-side url-location to it.
(rf/reg-fx
 :goto
 (fn [url]
   (secretary/dispatch! url)))

;; Initializes the client-side store with the definition from the `db` namespace
(rf/reg-event-db
 :initialize-db
 (fn  [_ _]
   db/default-db))

;; Sets the active panel (selected in the sidebar).
(rf/reg-event-db
 :set-active-panel
 (fn [db [_ active-panel]]
   (assoc db :active-panel active-panel)))

;; Toggles the client-side theme
(rf/reg-event-db
 :set-theme
 (fn [db [_]]
   (update-in db [:client :theme]
              util/toggle-value [:light :dark])))

;; Sets the data about the window size in the store
;; to the passed dimensions.
(rf/reg-event-db
 :set-window-size
 (fn [db [_ width height]]
   (assoc-in db [:client :window] {:width width
                                   :height height})))

;; Associates the given key with the given data inside
;; the test-part of the client-side store
(rf/reg-event-db
 :test
 (fn [db [_ key data]]
   (assoc-in db [:test key] data)))

;; Adds a message to the client side store, which will be rendered
;; by the view. If a timeout parameter is specified in the message
;; data, also initializes a timed-dispatch which will remove the
;; message from the store after the given timeout.
(rf/reg-event-fx
 :message/add
 (fn [{:keys [db]} [_ message]]
   (let [msg-id (str (random-uuid))
         other-params
         (if-let [timeout (:message/time message)]
           {:timed-dispatch [timeout [:message/remove msg-id]]}
           {})]
     (merge
      {:db (assoc-in db [:messages msg-id] message)}
      other-params))))

;; Removes the message with the given id from the store.
(rf/reg-event-db
 :message/remove
 (fn [db [_ id]]
   (update db :messages dissoc id)))

;; Displays a message that the (user-form) submission was invalid
(rf/reg-event-fx
 :submission/invalid
 (fn [w [_]]
   {:dispatch [:message/add (messages/invalid-submission-msg)]}))

;;
;; Server Events
;;

;; Send an echo message with the passed payload to the websocket.
(rf/reg-event-fx
 :server/echo-test
 (fn [_ [_ message]]
   {:ws [:testing/echo {:payload message}]}))

;; Pings the websocket server.
(rf/reg-event-fx
 :server/ping
 (fn [_]
   {:ws [client/ping-key]}))

;;
;; Authentication
;;

;; Handler for an 'unauthorized' response from the server.
;; Adds a message for the user, and logs out the user if necessary
(rf/reg-event-fx
 :auth/unauthorized
 (fn [w [_ data]]
   (if (contains? data :auth/no-login)
     {:dispatch-multiple [[:message/add (messages/no-login-system-msg (str data))]
                          [:auth/logout false]]}
     {:dispatch [:message/add (messages/no-permission-system-msg (str data))]})))

;; Logs out the user. Only if the passed argument `server-side?` is true,
;; also informs the server of the user logout. Otherwise, only deletes the
;; client-side token, the event-data and the game-state.
(rf/reg-event-fx
 :auth/logout
 (fn [w [_ server-side?]]
   (let [db-map {:db (-> (:db w)
                         (assoc :user {:auth false})
                         (assoc :events {})
                         (assoc :game-state {}))
                 :goto "/"}]
     (if server-side?
       (merge db-map {:ws [:auth/logout]})
       db-map))))

;; Sets the 'successful logout` flag in the store
(rf/reg-event-db
 :auth/logout-success
 (fn [db [_]]
   (assoc-in db [:user :logout-success] true)))

;; Forces the user to logout. Does not inform the server,
;; but displays a message to the user that they were logged
;; out.
(rf/reg-event-fx
 :auth/logout-forced
 (fn [w [_ data]]
   {:dispatch-multiple [[:auth/logout false]
                        [:message/add (messages/logout-forced-msg data)]]}))

;; Sends a 'login' message/request to the server.
(rf/reg-event-fx
 :auth/login
 (fn [_ [_ realm {:keys [:auth/key :event/id]}]]
   {:ws [:auth/login {:user/realm realm
                      :auth/key key
                      :event/id id}]}))

;; Sets out the 'successfully authorized' flags in the store,
;; and fetches the event-data associated with the event-id the user
;; logged in at.
(rf/reg-event-fx
 :auth/login-success
 (fn [{:keys [db]} [_ data-map]]
   {:db (update db :user merge data-map {:auth true
                                         :auth-failure false
                                         :logout-success true})
    :dispatch [:event/fetch (select-keys data-map [:event/id])]}))

;; Sets the flag in the store whether the client has an active connection
;; to the server (specified by the parameter `open?`.
(rf/reg-event-db
 :system/connection
 (fn [db [_ open?]]
   (assoc-in db [:client :connected] open?)))

;; Sets the flag in the store that the login credentials were invalid,
;; as well as an indication for the reason as passed with `data`
(rf/reg-event-db
 :auth/login-invalid
 (fn [db [_ data]]
   (update db :user merge {:auth-failure true
                           :auth-failure-reason data})))


;; Fetches the event-data for the event specified by `data`
(rf/reg-event-fx
 :event/fetch
 (fn [{:keys [db]} [_ data]]
   {:ws-auth [(:user db) [:event/fetch data]]}))

;; Associates the information passed with `data` in the store
;; as the event list.
(rf/reg-event-db
 :event/list
 (fn [db [_ data]]
   (assoc db :events data)))

;; Sends the 'create-event' message to the server.
(rf/reg-event-fx
 :event/create
 (fn [{:keys [db]} [_ data]]
   {:ws-auth [(:user db) [:event/create data]]}))

;; Sets the flag for given event of whether it was created successfully
(rf/reg-event-db
 :event/created
 (fn [db [_ data]]
   (if (:created data)
     (update db :events assoc (:event/id data) data)
     db)))

;; Sends a request to the server that the given event should be deleted.
(rf/reg-event-fx
 :event/destroy
 (fn [{:keys [db]} [_ {:keys [:event/id] :as data}]]
   {:ws-auth [(:user db) [:event/destroy {:event/id id}]]}))

;; Sets the flag in the store that the given event was deleted.
;; If it was not, displays a message of why it was not to the user
(rf/reg-event-fx
 :event/destroyed
 (fn [w [_ {:keys [:event/id :destroyed] :as data}]]
   (if destroyed
     {:db (update (:db w) :events dissoc id)}
     {:dispatch [:message/add (messages/event-not-destroyed-msg (str data))]})))

;; Sends a request to the server to start the event.
(rf/reg-event-fx
 :event/start
 (fn [{:keys [db]}
     [_ {:keys [:event/id]}]]
   {:ws-auth [(:user db) [:event/start {:event/id id}]]}))

;; Sets the flag in the store of wheter the event was started successfully
;; And displays a message informing the user that it was started (or not)
(rf/reg-event-fx
 :event/started
 (fn [{:keys [db]}
     [_ {:as event
         :keys [:event/started? :event/id]}]]
   (if started?
     {:db (update-in db [:events id] merge event)
      :dispatch [:message/add (messages/event-started-msg event)]}
     {:dispatch [:message/add (messages/event-not-started-msg event)]
      :db (update-in db [:events id] merge event)})))

;; Sets the currently selected event to the given event-id
(rf/reg-event-db
 :event/select
 (fn [db [_ event-id]]
   (assoc db :selected-event event-id)))

;; Associates the passed data in the store as the game data for
;; the given event (as specified by the event id)
(rf/reg-event-db
 :game/data
 (fn [db [_ data]]
   (let [event-id (or (:event/id data)
                      (get-in db [:user :event/id]))]
     (if (util/single-event? event-id)
       (assoc-in db [:events event-id :game/data]
                 (dissoc data :event/id))
       db
       #_(update db :events
                 reduce
                 (fn [coll [k v]]
                   (if (contains? data k)
                     (assoc coll k (get data k))
                     (assoc coll k v)))
                 {})))))

;; Requests a game-data update from the server
(rf/reg-event-fx
 :game/data-fetch
 (fn [{:keys [db]} [_]]
   {:ws-auth [(:user db) [:game/data-fetch]]}))

;; Informs the server that the player has commited the current round,
;; passing the given commit-data
(rf/reg-event-fx
 :game/round-commit
 (fn [{:keys [db]} [_ commit-data]]
   {:ws-auth [(:user db) [:game/round-commit commit-data]]}))

;; Informs the server that the player is ready for the next round
;; passing the given data
(rf/reg-event-fx
 :game/round-ready
 (fn [{:keys [db]} [_ ready-data]]
   {:ws-auth [(:user db) [:game/round-ready ready-data]]}))

;; Updates the client-side game data with the passed update-data,
;; if the given update was successful. If not, displays a message
;; informing the user that there was an error.
(rf/reg-event-fx
 :game/data-update
 (fn [{:keys [db]} [_ {:as update-data :keys [:game/updated?]}]]
   (if updated?
     (let [event-id (or (get update-data :event/id)
                        (get-in db [:user :event/id]))]
       (if (util/single-event? event-id)
         {:db (assoc-in db [:events event-id :game/data]
                        (dissoc update-data :event/id))}
         {:dispatch [:message/add (msgs/game-update-failed {:update/reason
                                                            {:event/id event-id}})]}))
     {:dispatch [:message/add (msgs/game-update-failed update-data)]})))

;; Sets the current round to 'acknowledged', which means the user
;; has acknowledged the data in the 'outgoing' part of the gui
(rf/reg-event-db
 :game/acknowledge-round
 (fn [db [_ round]]
   (assoc-in db [:game-state :acknowledgements round] true)))
