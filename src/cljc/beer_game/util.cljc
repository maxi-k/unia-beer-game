(ns beer-game.util)

(defn toggle-value
  "Returns the value from the two options that is not equal to `value`."
  [value [op1 op2]]
  (if (= value op1) op2 op1))

(defn split-keyword
  "Splits a qualified keyword (keyword with a namespace)
  into a vector of the form [namespace name], with both entries being keywords.
  If the keyword is not qualified, `namespace` will be nil."
  [kw]
  {:pre (keyword? kw)}
  (map keyword [(namespace kw) (name kw)]))

(defn name-only
  "Returns a keyword that contains only the name-part of given keyword,
  omitting the namespace."
  [kw]
  {:pre (keyword? kw)}
  (keyword (name kw)))
