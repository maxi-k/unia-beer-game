(ns beer-game.views.statistics
  (:require [reagent.core :as ra]
            [cljsjs.plotly]))

(def default-plot-options
  {:displaylogo false})

(defn draw-plot!
  "Draws a Plotly plot inside the element `ref`,
  with the given data `data`. `layout` options can be
  given optionally as a third argument."
  ([ref data]
   (draw-plot! ref data {}))
  ([ref data layout]
   (.plot js/Plotly
          ref
          ;; Data
          (clj->js data)
          ;; Layout Options
          (clj->js layout)
          ;; Plot Options
          (clj->js default-plot-options))))

(defn make-plot-component
  "Creates a react component given a function `plot-fn`
  that can draw a plot given a dom node. Takes a display-name
  for the created component as a optional first argument."
  ([plot-fn] (make-plot-component "automad-plot-component" plot-fn ))
  ([display-name plot-fn]
   (let [ref (atom nil)
         wrapped-plot-fn #(plot-fn @ref)]
     (ra/create-class
      {:display-name display-name
       :component-did-update wrapped-plot-fn
       :component-did-mount wrapped-plot-fn
       :reagent-render
       (fn []
         [:div {:ref (fn [com] (reset! ref com))}])}))))


(defn example-plot! [ref]
  (draw-plot! ref
              [{:x [1 2 3 4 5]
                :y [1 2 4 8 16]
                :margin {:t 0}}]))


(defn example-statistic []
  (let [plot (make-plot-component "example-statistic" example-plot!)]
    (fn []
      [:div
       [:h2 "Cool Plot: "]
       [plot]])))


(defn statistics-panel []
  (fn []
    [:section
     [:h1 "Statistiken"]
     [example-statistic]]))
