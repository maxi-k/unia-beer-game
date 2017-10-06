(ns beer-game.components.modals
  (:require [reagent.core :as ra]
            [soda-ash.core :as sa]
            [beer-game.client-util :as cutil]))

(defn generic-modal
  "A generic modal with title and, content components passed.
  Passes the modal state to the given subcomponents."
  [trigger title content options]
  (let [modal-state (ra/atom false)
        trigger-el (->> trigger
                        (cutil/with-options-raw
                          {:onClick #(reset! modal-state true)})
                        ra/as-element)]
    (fn []
      [sa/Modal (merge {:trigger trigger-el
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
