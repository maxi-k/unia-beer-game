(ns beer-game.components.plot
  (:require [cljsjs.plotly]
            [reagent.core :as ra]))

(def default-plot-options
  {:displaylogo false})

(defn draw-plot!
  "Draws a Plotly plot inside the element `ref`,
  with the given data `data`. `layout` options can be
  given optionally as a third argument."
  ([ref data]
   (draw-plot! ref data {}))
  ([ref data layout]
   (draw-plot! ref data layout {}))
  ([ref data layout options]
   (let [js-layout (clj->js layout)
         js-data (clj->js data)
         js-opts (clj->js (merge default-plot-options options))
         plot-res (.plot js/Plotly
                         ref
                         ;; Data
                         js-data
                         ;; Layout Options
                         js-layout
                         ;; Plot Options
                         js-opts)]

     #js {:layout js-layout
          :data js-data
          :options js-opts
          :plot plot-res})))

(defn- enrich-plot-options
  "'Enriches' the plot options by changing plot-specific options
  into options understandable by a normal element. For example:
  {:interactive true} -> {:class-name \"[old] interactive-plot \"}"
  [options]
  (cond-> options
    ;; -- if keyword 'interactive'
    (:interactive options)
    (->
     (dissoc :interactive)
     (update :class-name str " interactive-plot"))))

(defn make-plot-component
  "Creates a react component given a function `plot-fn`
  that can draw a plot given a dom node. Takes a display-name
  for the created component as a optional first argument."
  ([plot-fn] (make-plot-component "automade-plot-component" plot-fn))
  ([display-name plot-fn] (make-plot-component display-name {} plot-fn))
  ([display-name options plot-fn]
   (let [ref (atom nil)
         wrapped-plot-fn #(plot-fn @ref)]
     (ra/create-class
      {:display-name display-name
       :component-did-update wrapped-plot-fn
       :component-did-mount wrapped-plot-fn
       :component-will-unmount #(.purge js/Plotly @ref)
       :reagent-render
       (fn []
         [:div
          (merge (enrich-plot-options options)
                 {:ref (fn [com] (reset! ref com))})])}))))
