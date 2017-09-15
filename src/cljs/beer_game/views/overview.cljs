(ns beer-game.views.overview
  (:require [re-frame.core :as rf]
            [ajax.core :as ax :refer [GET POST]]
            [soda-ash.core :as sa]
            [beer-game.api :as api]))

(defn api-btn []
  (let [at (rf/subscribe [:test])
        content (:ws-echo @at)]
    [:div
     [sa/Button {:on-click #(api/echo-test! "Hello World!")}
      "Sockets!"]
     [:p [:strong (str content)]]]))

(defn overview-panel []
  (let [name (rf/subscribe [:name])]
    (fn []
      [:div (str "Hello from " @name ". This is the Home Page.")
       [:div [:a {:href "#/about"} "go to About Page"]]
       [api-btn]])))
