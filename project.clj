(defproject beer-game "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0-beta1"]
                 [org.clojure/clojurescript "1.9.908"]
                 [com.stuartsierra/component "0.3.2"]
                 [yogthos/config "0.8"]
                 [com.taoensso/sente "1.11.0"]
                 [compojure "1.5.0"]
                 [ring "1.5.0"]
                 [ring-middleware-format "0.7.2"]
                 [http-kit "2.2.0"]
                 [reagent "0.7.0"]
                 [re-frame "0.10.1"]
                 [re-frisk "0.4.5"]
                 [secretary "1.2.3"]
                 [soda-ash "0.4.0"]]

  :plugins [[refactor-nrepl "2.3.1"]
            [cider/cider-nrepl "0.15.1"]
            [lein-cljsbuild "1.1.5"]
            [lein-shell "0.5.0"]
            [lein-auto "0.1.3"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj" "src/cljc"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :auto {"less" {:paths ["src/less"]
                 :file-pattern #"\.(less|config)$"}}

  ;; Requires less and less-clean-css to be installed
  :aliases {"less" ["shell" "lessc" "src/less/site.less" "resources/public/css/site.css"
                    "--clean-css" "--s1 --advanced --compatibility=ie8"]
            "dev-repl" ["with-profile" "dev" "repl"]}


  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]
                 :timeout 120000}

  :figwheel {:css-dirs ["resources/public/css"]
             :reload-clj-files {:clj true :cljc true}
             :server-logfile false
             :ring-handler :beer-game.components.handler/handler-fn}

  :profiles
  {:dev {:dependencies [[binaryage/devtools "0.9.4"]
                        [figwheel-sidecar "0.5.13"]
                        [com.cemerick/piggieback "0.2.2"]]
         :plugins      [[lein-figwheel "0.5.13"]]
         :resource-paths ["environments/dev"]}
   :prod {:resource-paths ["environments/prod"]}}

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljs" "src/cljc" "environments/dev"]
     :figwheel     {:on-jsload "beer-game.core/mount-root"}
     :compiler     {:main                 beer-game.core
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :asset-path           "js/compiled/out"
                    :source-map-timestamp true
                    :preloads             [devtools.preload]
                    :external-config      {:devtools/config {:features-to-install :all}}
                    :closure-warnings {:extra-require :warning}}}


    {:id           "min"
     :source-paths ["src/cljs" "src/cljc"]
     :jar true
     :compiler     {:main            beer-game.core
                    :output-to       "resources/public/js/compiled/app.js"
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :closure-warnings {:extra-require :warning}
                    :pretty-print    false}}]}

  :main beer-game.server

  :aot [beer-game.server]

  :uberjar-name "beer-game.jar"

  :prep-tasks [["cljsbuild" "once" "min"]["less"] "compile"])
