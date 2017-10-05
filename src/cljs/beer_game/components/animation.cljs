(ns beer-game.components.animation
  (:require [reanimated.core :as anim]
            [reagent.core :as ra]))

(defn transition-group
  [{:keys [name enter-time leave-time component class]
    :or [name "transition-item"
         enter-time 500
         leave-time 500
         component "div"
         class ""]}
   children]
  [anim/css-transition-group
   {:transition-name name
    :transition-enter-timeout enter-time
    :transition-leave-timeout leave-time
    :component (str class " transition-group-wrapper")}
   children])
