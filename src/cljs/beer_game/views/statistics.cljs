(ns beer-game.views.statistics
  (:require [reagent.core :as ra]
            [cljsjs.plotly]))

(def default-plot-options
  {:displaylogo false})

(defn example-plot! [ref]
  (.plot js/Plotly
         ref
         ;; Data
         (clj->js [{:x [1 2 3 4 5]
                    :y [1 2 4 8 16]
                    :margin {:t 0}}])
         ;; Layout Options
         #js {}
         ;; Plot Options
         (clj->js default-plot-options)))

(defn example-statistic []
  (let [ref (atom nil)
        plot-fn #(example-plot! @ref)]
    (ra/create-class
     {:display-name "example-statistic"
      :component-did-update plot-fn
      :component-did-mount plot-fn
      :reagent-render
      (fn []
        [:div
         "Plot: "
         [:div {:ref (fn [com] (reset! ref com))}]])})))

(defn statistics-panel []
  (fn []
    [example-statistic]))
