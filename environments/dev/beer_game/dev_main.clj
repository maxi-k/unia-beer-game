(ns beer-game.dev-main
  (:require [figwheel-sidecar.repl-api :as ra]
            [com.stuartsierra.component :as component]
            [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]
            [beer-game.message-handler :refer [message-handler]]
            [beer-game.components
             [websocket :refer [new-websocket]]
             [routes :refer [new-routes]]
             [handler :refer [new-handler]]])
  (:gen-class))

(defn development-system
  "The system map for the development server ."
  []
  (component/system-map
   :websocket (new-websocket get-sch-adapter message-handler)
   :routes    (component/using (new-routes)       [:websocket])
   :handler   (component/using (new-handler true) [:routes])))

(def dev-build
  {:id           "dev"
   :source-paths ["src/cljs" "src/cljc" "environments/dev"]
   :figwheel     {:on-jsload "beer-game.core/mount-root"}
   :compiler     {:main                 "beer-game.core"
                  :output-to            "resources/public/js/compiled/app.js"
                  :output-dir           "resources/public/js/compiled/out"
                  :asset-path           "js/compiled/out"
                  :source-map-timestamp true
                  :closure-warnings {:extra-require :warning}}})

(def figwheel-config
  {:build-ids ["dev"]
   :figwheel-options {:css-dirs ["resources/public/css"]
                      :reload-clj-files {:clj true :cljc true}
                      :server-logfile false}
   :all-builds
   [dev-build]})

(def system-status
  (atom nil))

(defn init []
  (reset! system-status (development-system)))

(defn start []
  (swap! system-status component/start)
  (ra/start-figwheel! (assoc-in figwheel-config [:figwheel-options :ring-handler]
                                (-> @system-status :handler :handler-fn))))

(defn stop []
  (swap! system-status
         (fn [s] (when s (component/stop s))))
  (ra/stop-figwheel!))

(defn cljs [] (ra/cljs-repl "dev"))

(defn go []
  (init)
  (start))

(defn -main
  "Entrypoint for the development system."
  [& args])
