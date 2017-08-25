(ns beer-game.views.overview
  (:require [re-frame.core :as re-frame]))

(defn overview-panel []
  (let [name (re-frame/subscribe [:name])]
    (fn []
      [:div (str "Hello from " @name ". This is the Home Page.")
       [:div [:a {:href "#/about"} "go to About Page"]]])))
