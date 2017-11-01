(ns beer-game.components.tables
  (:require [soda-ash.core :as sa]))

(defn definition-table
  "A table for displaying some data-definition passed as a map."
  ([data-map] (definition-table data-map {}))
  ([data-map options]
   [sa/Table (merge options {:definition true})
    [sa/TableBody
     (for [[key value] data-map]
       [sa/TableRow {:key key}
        [sa/TableCell key]
        [sa/TableCell value]])]]))
