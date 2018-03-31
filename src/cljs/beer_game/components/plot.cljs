(ns beer-game.components.plot
  "Components for drawing plots using Plotly."
  (:require [cljsjs.plotly]
            [reagent.core :as ra]
            [beer-game.util :as util]))

(def default-plot-options
  "The default plot options that are shared by all plots."
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



(defn setup-drag-behavior!
  "Sets up the drag behavior for the [[interactive-line-plot]] using
  the d3 instance of Plotly and its drag behavior utilities.
  Requires a reference `ref` of the element containing the plot,
  an atom(-like) `y-atom` for reading and updating the de-facto plot values,
  as well as a boolean argument `update-all?` which indicates whether all future
  plot points after the dragged one should be updated."
  [ref y-atom update-all?]
  (let [d3 (.-d3 js/Plotly)
        drag (-> d3 .-behavior .drag)
        tmp-data (atom {:y @y-atom
                        :mouse-frac 0.5
                        :updated-point nil})
        commit-drag!
        (fn [invert-all?]
          (if (util/bool-xor update-all? invert-all?)
            (let [point-idx (:updated-point @tmp-data)
                  new-val (max 0 (.round js/Math (get-in @tmp-data [:y point-idx])))]
              (reset! y-atom
                      (vec (map-indexed (fn [idx item]
                                          (if (>= idx point-idx)
                                            new-val
                                            item))
                                        (:y @tmp-data)))))
            (reset! y-atom
                    (mapv (fn [p] (max 0 (.round js/Math p)))
                          (:y @tmp-data)))))]
    (.origin drag
             (fn []
               (this-as this
                 (let [transform (-> d3 (.select this) (.attr "transform"))
                       translate (-> transform
                                     (.substring 10 (dec (.-length transform)))
                                     (.split ","))]
                   (clj->js {:x (aget translate 0)
                             :y (aget translate 1)})))))
    (.on drag "dragstart"
         (fn []
           (this-as this
             (let [transform (-> d3 (.select this) (.attr "transform"))
                   translate (-> transform
                                 (.substring 10 (dec (.-length transform)))
                                 (.split ","))]
               (set! (.-originalCoords this) (clj->js {:x (aget translate 0)
                                                       :y (aget translate 1)})))
             )))
    (.on drag "drag"
         (fn []
           (this-as this
             (let [ymouse (-> d3 .-event .-y)
                   [xorig yorig] [(-> this .-originalCoords .-x)
                                  (-> this .-originalCoords .-y)]
                   client-height (try (-> ref (.getElementsByClassName "draglayer")
                                          (aget 0)
                                          .-firstChild
                                          .-firstChild
                                          (.getAttribute "height")
                                          js/parseInt)
                                      (catch js/Error e 0))
                   point (aget this "__data__")
                   y-px (if (zero? client-height)
                          (:mouse-frac @tmp-data)
                          (/ (- client-height ymouse) client-height))
                   val-range (-> ref .-layout .-yaxis .-range)
                   range-sum (- (aget val-range 1)
                                (aget val-range 0))
                   val-pos (+ (aget val-range 0)
                              (* range-sum y-px))]
               #_(.log js/console ref (-> d3 .-event) ymouse y-px val-pos)
               (swap! tmp-data assoc :updated-point (.-i point))
               (swap! tmp-data assoc :mouse-frac y-px)
               (swap! tmp-data assoc-in [:y (.-i point)] val-pos)
               (when (>= val-pos 0)
                 (-> d3 (.select this)
                     (.attr "transform"
                            (str "translate("xorig "," (max 0 ymouse) ")"))))))))
    (.on drag "dragend"
         (fn [] (this-as this
                 (commit-drag! (-> d3 .-event (aget "sourceEvent") .-shiftKey))
                 (js-delete this "originalCoords"))))
    (-> d3
        (.selectAll "path.point")
        (.call drag))))

(defn interactive-line-plot
  "Creates an interactive line plot, where the values can be dragged
  in the y direction. Needs to be passed the `x-values` of the plot as a vector
  as well as an IAtom for the `y-values`. The optional first argument is an option map
  passed to the surrounding :div element. It can contain several options used otherwise:
  `:plot-update-after` A function that is called after the plot updates.
  It is passed the plot element ref.
  `:plot-update-before` A function that is called before the plot updates.
  It is passed the plot element ref.
  `:xaxis`, `:yaxis`, `:title` Options passed to the layout of the graph.
  They are merged with default options."
  ([x-values y-atom]
   (interactive-line-plot {} x-values y-atom))
  ([{:as options :keys [plot-update-before plot-update-after]}
    x-values y-atom]
   (let [update-all? (ra/atom false)
         special-options [:plot-update-before :plot-update-after
                          :xaxis :yaxis :title]]
     (fn [{:as options :keys [plot-update-before plot-update-after]}
         x-values y-atom]
       (let [plot-fn (fn [ref]
                       (when (fn? plot-update-before) (plot-update-before ref))
                       (draw-plot!
                        ref
                        [{:x x-values
                          :y @y-atom
                          :mode "lines+markers"
                          :marker {:size 12}
                          :hoverinfo "text"
                          :text (map (fn [x y] (str "Runde " x " - Anfrage " y))
                                     x-values @y-atom)
                          :name "interactive-plot"}]
                        {:title (:title options)
                         :xaxis (merge {:fixedrange true} (:xaxis options) )
                         :yaxis (merge {:fixedrange true
                                        :range (util/padded-data-range @y-atom)}
                                       (:yaxis options))
                         :margin {:pad 10}}
                        {:displayModeBar false
                         :scrollZoom false})
                       (setup-drag-behavior! ref y-atom @update-all?)
                       (when (fn? plot-update-after) (plot-update-after ref)))
             plot (make-plot-component
                   "interactive"
                   {:interactive true}
                   plot-fn)]
         [:div (apply dissoc options special-options)
          [plot]
          [:label
           [:input {:type :checkbox
                    :style {:margin-right "0.5rem"
                            :margin-top "4px"}
                    :value @update-all?
                    :on-change #(reset! update-all? (-> % .-target .-checked))}]
           "Alle zukünftigen ändern? (= Shift)"]
          [:br]
          [:p "Shift+Klick auf einen Punkt zum zurücksetzen."]])))))
