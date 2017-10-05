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
                 [adzerk/boot-cljs-repl       "0.3.3"  :scope "test"]
                 [com.cemerick/piggieback     "0.2.2"  :scope "test"]
                 [weasel                      "0.7.0"  :scope "test"]
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
 '[clojure.java.io       :as io]
 '[adzerk.boot-cljs      :refer [cljs]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl repl-env]]
 '[adzerk.boot-reload    :refer [reload]]
 '[system.boot           :refer [system run]]
 '[beer-game.server      :refer [server-system]])

(task-options!
 pom {:project 'beer-game
      :version "0.1.0"}
 jar {:manifest {"author" "Maximilian Kuschewski"}
      :main 'beer-game.server}
 aot {:namespace #{'beer-game.server}})

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

(deftask less-js
  "Compiles the less files using lessc,
  because boot-less / less4j can't seem to handle compiling Semantic-UI.
  (GC Overflow error)."
  [i  input   str "The input file path"
   o  output  str "The output file path"]
  (let [todir (tmp-dir!)
        prev (atom nil)]
    (with-pre-wrap fileset
      (let [candidates (->> fileset
                           input-files
                           (by-ext [".less" ".config" ".overrides"]))
            changed-files (fileset-diff @prev candidates)
            in-file (first (by-path [input] candidates))
            tmp-in (tmp-file in-file)
            tmp-out (io/file todir output)]
        (when (seq changed-files)
          (empty-dir! todir)
          (println "Compiling Less...")
          (dosh "lessc" (.getPath tmp-in) (.getPath tmp-out)
                "--clean-css" "--s1 --advanced --compatibility=ie8"))
        (reset! prev candidates)
        (-> fileset
            (add-resource todir)
            commit!)))))

(deftask dev
  "Start a repl for development with auto-watching etc..."
  []
  (comp
   (dev-env)
   (watch :verbose true)
   (system :sys #'server-system :auto true)
   (reload :on-jsload 'beer-game.core/mount-root)
   (repl :server true)
   (cljs :source-map true
         :optimizations :none)
   (less-js :input "site.less"
            :output "public/css/site.css")
   (notify :visible true
           :audible false)))

(deftask package
  []
  (comp
   (prod-env)
   (cljs :optimizations :advanced
         :compiler-options {:pretty-print false
                            :preloads nil})
   (less-js :input "site.less"
            :output "public/css/site.css")
   (aot)
   (pom)
   (uber)
   (jar :file "beer-game.jar")
   (sift :include #{#".*\.jar"})
   (target)
   (notify :audible true
           :visible true)))
