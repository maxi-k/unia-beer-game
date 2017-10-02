(require
 '[figwheel-sidecar.repl-api :as ra]
 '[com.stuartsierra.component :as component]
 '[ring.component.jetty :refer [jetty-server]]
 '[beer-game.server :as app])

(def dev-profile
  {:id           "dev"
   :source-paths ["src/cljs" "src/cljc"]
   :figwheel     {:on-jsload "beer-game.core/mount-root"}
   :compiler     {:main                 beer-game.core
                  :output-to            "resources/public/js/compiled/app.js"
                  :output-dir           "resources/public/js/compiled/out"
                  :asset-path           "js/compiled/out"
                  :source-map-timestamp true
                  :preloads             [devtools.preload]
                  :external-config      {:devtools/config {:features-to-install :all}}
                  :closure-warnings {:extra-require :warning}}})

(def figwheel-config
  {:figwheel-options {} ;; <-- figwheel server config goes here
   :build-ids ["dev"]   ;; <-- a vector of build ids to start autobuilding
   :all-builds          ;; <-- supply your build configs here
   [dev-profile]})

(defrecord Figwheel [config handler]
  component/Lifecycle
  (start [this]
    (let [handler-fn (if (fn? handler) handler (:handler handler))
          merged-config (assoc config :ring-handler handler-fn)
          fw (ra/start-figwheel! merged-config)]
      (-> this
          (assoc :merged-config merged-config)
          (assoc :figwheel figwheel))))
  (stop [config]
    ;; you may want to restart other components but not Figwheel
    ;; consider commenting out this next line if that is the case
    (ra/stop-figwheel!)
    (-> this
        (dissoc :figwheel))))

(def system
  (atom
   (merge (app/server-system)
          (component/system-map
           :figwheel (component/using
                      (map->Figwheel {:config figwheel-config})
                      [:handler])))))

(defn start []
  (swap! system component/start))

(defn stop []
  (swap! system component/stop))

(defn reload []
  (stop)
  (start))

(defn repl []
  (ra/cljs-repl))
