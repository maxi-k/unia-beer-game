(ns beer-game.views.devcards
  "A devcards view for various components and views in the development stage,
  which can be developed in concept here before having to be integrated into the
  the app and connected to the client-store."
  (:require [reagent.core :as ra]
            [soda-ash.core :as sa]
            [beer-game.util :as util]
            [beer-game.client-util :as cutil]
            [beer-game.components.plot :as plot]))

(defn padded-data-range
  "Returns the range of numbers contained within given
  collection `coll`, padded on each side with `padding`
  as a vector of two numbers [min-num max-num]."
  ([coll] (padded-data-range coll 4))
  ([coll padding]
   [(- (apply min coll) padding)
    (+ (apply max coll) padding)]))

(defn setup-drag-behavior! [ref data-atom update-all?]
  (let [d3 (.-d3 js/Plotly)
        drag (-> d3 .-behavior .drag)
        tmp-data (atom {:y (:y @data-atom)
                        :mouse-frac 0.5
                        :updated-point nil})
        commit-drag!
        (fn [invert-all?]
          (if (util/bool-xor update-all? invert-all?)
            (let [point-idx (:updated-point @tmp-data)
                  new-val (max 0 (.round js/Math (get-in @tmp-data [:y point-idx])))]
              (swap! data-atom assoc :y
                     (vec (map-indexed (fn [idx item]
                                         (if (>= idx point-idx)
                                           new-val
                                           item))
                                       (:y @tmp-data)))))
            (swap! data-atom assoc :y
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
                   point (-> this .-__data__)
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
               (-> d3 (.select this) (.attr "transform"
                                            (str "translate("xorig "," (max 0 ymouse) ")")))))))
    (.on drag "dragend"
         (fn [] (this-as this
                 (commit-drag! (-> d3 .-event .-sourceEvent .-shiftKey))
                 (js-delete this "originalCoords"))))
    (-> d3
        (.selectAll "path.point")
        (.call drag))))

(defn interactive-plot
  []
  (let [data-atom (ra/atom {:x [1 2 3 4 5]
                            :y [5 5 5 5 5]
                            :rounds 5})
        update-all? (ra/atom false)
        plot-values! (fn [val]
                       (let [parsed (js/parseInt val)
                             rounds(if (pos-int? parsed) parsed 5)]
                         (swap! data-atom merge
                                {:x (vec (range rounds))
                                 :y (vec (take rounds (repeat 5)))
                                 :rounds parsed})))
        plot-value! (fn [idx val]
                      (let [parsed (js/parseInt val)
                            yvalue (if (nat-int? parsed) parsed 0)]
                        (swap! data-atom assoc-in [:y idx] yvalue)
                        (swap! data-atom assoc-in [:last-point :value]
                               (if (pos-int? parsed) parsed ""))))]
    (fn []
      (let [plot-fn (fn [ref]
                      (plot/draw-plot!
                       ref
                       [{:x (:x @data-atom)
                         :y (:y @data-atom)
                         :mode "lines+markers"
                         :marker {:size 12}
                         :hoverinfo "text"
                         :text (map (fn [x y] (str "Runde " x " - Anfrage " y))
                                    (:x @data-atom) (:y @data-atom))
                         :name "interactive-plot"}]
                       {:title "Kundennachfrage"
                        :xaxis {:fixedrange true
                                :title "Runden"}
                        :yaxis {:fixedrange true
                                :title "Kundennachfrage"
                                :range (padded-data-range (:y @data-atom))}
                        :margin {:pad 10}}
                       {:displayModeBar false
                        :scrollZoom false})
                      (setup-drag-behavior! ref data-atom @update-all?)
                      (swap! data-atom assoc :updated-point nil))
            plot (plot/make-plot-component
                  "interactive"
                  {:interactive true}
                  plot-fn)]
        [:div
         [:h1 "Interactive-plot"]
         [:input {:on-change #(plot-values! (-> % .-target .-value))
                  :value (if (pos-int? (:rounds @data-atom))
                           (:rounds @data-atom)
                           "")}]
         [:label
          [:input {:type :checkbox
                   :value @update-all?
                   :on-change #(reset! update-all? (-> % .-target .-checked))}]
          "Alle zukünftigen ändern? (= 'Shift')"]
         [plot]]))))


(defn devcards-panel []
  [interactive-plot])
