(ns beer-game.components.animation
  "General-Purpose Components for animation."
  (:require [reanimated.core :as anim]
            [reagent.core :as ra]))

(defn transition-group
  "Creates a transition group with the given children,
  where the options passed in the option-map are used
  as parameters for the css-transition-group."
  [{:keys [name enter-time leave-time component class]}
   children]
  [anim/css-transition-group
   {:transition-name (or name "transition-item")
    :transition-enter-timeout (or enter-time 500)
    :transition-leave-timeout (or leave-time 500)
    :component (or component "div")
    :class (str class " transition-group-wrapper")}
   children])
