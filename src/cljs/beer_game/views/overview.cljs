(ns beer-game.views.overview
  (:require [re-frame.core :as rf]
            [soda-ash.core :as sa]
            [beer-game.api :as api]))

(defn overview-panel []
  (let [name (rf/subscribe [:name])]
    [:div]))
