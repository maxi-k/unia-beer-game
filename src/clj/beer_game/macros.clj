(ns beer-game.macros
  (:require [clojure.java.io :as io]))

(defmacro read-resource
  "Reads given resource at compile time (!)
  and tries to read it as clojure/edn."
  [resource]
  (-> resource
      io/resource
      slurp
      read-string))
