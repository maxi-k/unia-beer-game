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

(defn interpose-indexed
  "Like interpose, but takes a function instead of a seperator.
  This function is called with the index of the current call."
  [sep-fn coll]
  (drop 1
        (interleave
         (map sep-fn (range))
         coll)))

;; DOMAIN SPECIFIC

(def all-event-ids
  #{:all :event/all})

(defn multiple-events?
  "Returns true if given event-id refers to multiple events."
  [id]
  (contains? all-event-ids id))

(def single-event?
  "Returns true if given event-id refers to a single event."
  (comp not multiple-events?))

(defn users-by-event
  "Takes a map from user-id to user-data and reorders it to
  a map from event-id -> { user-id -> user-data }."
  [user-map]
  (reduce
   (fn [coll [user-id {:as user-data
                      event-id :event-id}]]
     (update coll event-id assoc user-id user-data))
   {} user-map))

(defn apply-transformations
  "Takes a map `data` and a map `transforms` that maps from key to a transformative function.
  For every item in the original map, tries to transform it using the
  function associated with the same key in the `transforms`-map. If
  there is no such key, assumes `default-transform`, which itselfs
  defaults to `identity`."
  ([data transforms] (apply-transformations data transforms identity))
  ([data transforms default-transform]
   (reduce
    (fn [coll [k v]]
      (if-let [trafo (get transforms k)]
        (assoc coll k (trafo v))
        (assoc coll k (default-transform v))))
    {}
    data)))

(defn filter-round-data
  "Takes a vector of round-data and returns a vector of round-data
  where the data for reach round only contains the information
  for the given user-role."
  [rounds user-role]
  (if (or (nil? rounds) (nil? user-role))
    nil
    (map
     (fn [round]
       (update round :game/roles
               select-keys [user-role]))
     rounds)))
