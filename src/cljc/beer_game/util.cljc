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
  {:pre [(keyword? kw)]}
  (map keyword [(namespace kw) (name kw)]))

(defn name-only
  "Returns a keyword that contains only the name-part of given keyword,
  omitting the namespace."
  [kw]
  {:pre [(keyword? kw)]}
  (keyword (name kw)))


#?(:clj
   (when-not (resolve 'qualified-keyword?)
     (defn qualified-keyword?
       "Returns true if given keyword is a qualified keyword (with namespace)."
       [kw]
       (if (re-find #"/" (str kw)) true false))))

#?(:clj
   (when-not (resolve 'simple-keyword?)
     (def simple-keyword?
       "Returns true if given keyword is a simple keyword (without namespace)."
       (comp not qualified-keyword?))))

(defn keyword->string
  "Turns a (qualified) keyword into a string,
  which can be turned into an equal keyword again with `clojure.core/keyword`"
  [kw]
  {:pre [(keyword? kw)]}
  (if (qualified-keyword? kw)
    (str (namespace kw) "/" (name kw))
    (name kw)))

(defn map->nsmap
  "Takes a map `m` and a namespace `n` and
  prefixes every simple keyword key in that map
  with given namespace."
  [m n]
  (reduce-kv (fn [acc k v]
               (let [new-kw (if (and (keyword? k)
                                     (not (qualified-keyword? k)))
                              (keyword (str n) (name k))
                              k) ]
                 (assoc acc new-kw v)))
             {} m))

(defn users-by-event
  "Takes a map from user-id to user-data and reorders it to
  a map from event-id -> { user-id -> user-data }."
  [user-map]
  (reduce
   (fn [coll [user-id {:as user-data
                      event-id :event-id}]]
     (update coll event-id assoc user-id user-data))
   {} user-map))
