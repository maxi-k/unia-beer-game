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

(defn semantic-to-react
  "Turns a semantic-ui component provided by soda-ash
  into something react-compatible
  which can be passed to other react-components."
  [semantic-component]
  (.-name semantic-component))

(defn native-render-fn
  "Takes a reagent-style component and returns a react-style
  render function that behaves like a normal js function."
  [component]
  (let [res (-> component
                ra/reactify-component
                ra/create-element)]
    (fn [] res)))

(defn with-options
  "Returns the given component with the given option map merged."
  [options component]
  (fn [own-options & children]
    [component (merge own-options options) children]))
