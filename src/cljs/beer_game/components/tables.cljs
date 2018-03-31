(ns beer-game.components.tables
  "Components for rendering tables."
  (:require [soda-ash.core :as sa]))

(defn definition-table
  "A table for displaying some data-definition passed as a map,
  where each key-value pair in the `data-map` is rendered side-by-side
  as table cells in the same row. The optional `options` are passed
  as-is to the underlying table Component.."
  ([data-map] (definition-table data-map {}))
  ([data-map options]
   [sa/Table (merge options {:definition true})
    [sa/TableBody
     (for [[key value] data-map]
       [sa/TableRow {:key key}
        [sa/TableCell key]
        [sa/TableCell value]])]]))
