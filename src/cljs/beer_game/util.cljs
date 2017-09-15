(ns beer-game.util
  (:require [goog.events :as events]
            [reagent.core :as ra]))

(defn keyify
  "Takes a seq of items and adds the 'key' metadata to it,
  using the element sindex in the array or given function.
  Used to satisfy the :key-prop restraint from react."
  ([items f]
   (map #(with-meta % (f %)) items))
  ([items]
   (map-indexed #(with-meta %2  {:key %1}) items)))

(defn toggle-value
  [value [op1 op2]]
  (if (= value op1) op2 op1))

(defn event-listen
  "Listen to events of type `type` on `target`,
  with given `callback`."
  [target type callback]
  (events/listen target (name type) callback))

(defn event-trigger
  "Trigger the event given by `type` on `target`"
  [target type]
  (events/dispatchEvent target (name type)))

(defn native-render-fn
  [component]
  (let [res (-> component
                ra/reactify-component
                ra/create-element)]
    (fn [] res)))
