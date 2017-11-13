(ns beer-game.views.imprint
  (:require [soda-ash.core :as sa]))

(def images
  [{:creator {:name "Freepik"
              :link "www.freepik.com"}
    :source {:name "Flaticon"
             :link "www.flaticon.com"}
    :license {:name "Creative Commons 3.0 BY"
              :link "http://creativecommons.org/licenses/by/3.0/"}}])

(defn imprint-images
  []
  [:div
   [sa/ListSA {:divided true :relaxed true}
    (for [{:as img :keys [source creator license]} images]
      [sa/ListItem {:key source}
       [:p "Von: " [:a {:href (:link creator)}
                       (:name creator)]]
       [:p "Quelle: " [:a {:href (:link source)}
                          (:name source)]]
       [:p "Lizenz: " [:a {:href (:link license)}
                          (:name license)]]])]])



(defn imprint-panel
  []
  [:div
   [:h2 "Impressum"]
   [:section
    [:h3 "Bilder und Icons"]
    [imprint-images]]])
