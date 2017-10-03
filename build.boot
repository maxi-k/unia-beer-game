(set-env!
 :source-paths #{"src/clj" "src/cljc" "src/cljs" "src/less"}
 :resource-paths #{"resources"}
 :dependencies '[[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.908"]

                 ;; Build dependencies
                 [org.clojure/tools.namespace "0.3.0-alpha4" :scope "test"]
                 [org.clojure/tools.nrepl     "0.2.12" :scope "test"]
                 [adzerk/boot-cljs            "2.1.4"  :scope "test"]
                 [adzerk/boot-reload          "0.5.2"  :scope "test"]
                 [deraen/boot-less            "0.6.2"  :scope "test"]
                 [com.cemerick/piggieback     "0.2.2"  :scope "test"]
                 [reloaded.repl               "0.2.3"  :scope "test"]
                 [binaryage/devtools          "0.9.4"  :scope "test"]
                 ;; Other development dependencies
                 [org.danielsz/system "0.4.0"]

                 ;; Project dependencies
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
                 [soda-ash "0.4.0"]])

(require
 '[adzerk.boot-cljs   :refer [cljs]]
 '[adzerk.boot-reload :refer [reload]]
 '[deraen.boot-less   :refer [less]]
 '[system.boot        :refer [system run]]
 '[beer-game.server   :refer [server-system]])

(task-options!
 pom {:project 'beer-game
      :version "0.1.0"}
 jar {:manifest {"author" "Maximilian Kuschewski"}
      :main 'beer-game.server}
 aot {:namespace #{'beer-game.server}}
 less {:source-map true})

(deftask dev-env
  "Sets the environment variables and paths for the development environment."
  []
  (set-env! :resource-paths #(conj % "environments/dev"))
  (set-env! :source-paths #(conj % "test" "environments/dev"))
  identity)

(deftask prod-env
  "Sets the environment variables and paths for the produciton environment."
  []
  (set-env! :resource-paths #(conj % "environments/prod"))
  (set-env! :source-paths #(conj % "environments/prod"))
  identity)

(deftask dev
  "Start a repl for development with auto-watching etc..."
  []
  (comp
   (dev-env)
   (watch :verbose true)
   (system :sys #'server-system :auto true)
   (reload :on-jsload 'beer-game.core/mount-root)
   (less)
   (cljs :source-map true
         :optimizations :none)
   (repl :server true)))

(deftask package
  []
  (comp
   (prod-env)
   (less :compression true)
   (cljs :optimizations :advanced
         :compiler-options {:pretty-print false
                            :preloads nil})
   (aot)
   (pom)
   (uber)
   (jar :file "beer-game.jar")
   (sift :include #{#".*\.jar"})
   (target)))
