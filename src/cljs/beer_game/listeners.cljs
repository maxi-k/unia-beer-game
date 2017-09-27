(ns beer-game.listeners
  (:require [re-frame.core :as rf]
            [beer-game.client-util :as util]))


(defn init-listeners
  "Initialize global listeners, such as window-size."
  []
  (letfn [(update-window-size [e]
            (rf/dispatch [:set-window-size
                          (.. e -target -innerWidth)
                          (.. e -target -innerHeight)]))]
    (util/event-listen js/window :resize update-window-size)
    (update-window-size (clj->js {:target js/window}))))
