(ns beer-game.server-utils
  (:require [clojure.java.io :as io]))

(defmacro read-resource
  "Reads given resource at compile time (!)
  and tries to read it as clojure/edn."
  [resource]
  (-> resource
      io/resource
      slurp
      read-string))

(defn uuid [] (str (java.util.UUID/randomUUID)))
