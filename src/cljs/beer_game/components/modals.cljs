(ns beer-game.components.modals
  "Components for displaying modals (dialogs that are
  displayed on top of the rest of the page)."
  (:require [reagent.core :as ra]
            [soda-ash.core :as sa]
            [beer-game.client-util :as cutil]))

(defn generic-modal
  "A generic modal with title and, content components passed.
  Passes the modal state to the given subcomponents.
  The reagent trigger element vector has to have an options map."
  [trigger title content options]
  (let [modal-state (ra/atom false)
        wrap-trigger (fn [t]
                       (if (fn? t)
                         (ra/as-element
                          (t #(reset! modal-state true)))
                         (->> t
                              (cutil/with-options-raw
                                {:onClick #(reset! modal-state true)})
                              ra/as-element)))]
    (fn [trigger title content options]
      [sa/Modal (merge {:trigger (wrap-trigger trigger)
                        :dimmer :blurring
                        :open @modal-state} options)
       [sa/Icon {:name "close"
                 :onClick #(reset! modal-state false)}]
       [sa/ModalHeader
        (if (string? title)
          title
          [title modal-state])]
       [sa/ModalContent
        (if (string? content)
          content
          [content modal-state])]])))
