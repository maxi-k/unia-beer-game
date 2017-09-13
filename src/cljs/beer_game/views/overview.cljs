(ns beer-game.views.overview
  (:require [re-frame.core :as rf]
            [ajax.core :as ax :refer [GET POST]]
            [soda-ash.core :as sa]))

(defn ajax-test []
  (letfn [(handler [response]
            (.log js/console (str response))
            (rf/dispatch [:do-ajax-test :simple response]))]
    (GET "/api/action"
         {:handler handler
          :error-handler handler
          :format (ax/json-request-format)
          :response-format (ax/json-response-format {:keywords? true})})))

(defn api-btn []
  (let [at (rf/subscribe [:test/ajax])
        content (:simple @at)]
    [:div
     [sa/Button {:on-click ajax-test}
      "Ajax!"]
     [:p [:strong (str content)]]]))

(defn overview-panel []
  (let [name (rf/subscribe [:name])]
    (fn []
      [:div (str "Hello from " @name ". This is the Home Page.")
       [:div [:a {:href "#/about"} "go to About Page"]]
       [api-btn]])))
