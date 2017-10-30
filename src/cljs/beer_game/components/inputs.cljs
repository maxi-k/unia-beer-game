(ns beer-game.components.inputs
  (:require [clojure.spec.alpha :as spec]
            [soda-ash.core :as sa]
            [reagent.core :as ra]
            [re-frame.core :as rf]))

(defn make-validated-input
  "Creates the data required for a validated-input-element."
  [{:as options
    :keys [spec as invalid-msg transform on-change]
    :or {spec (constantly true)
         on-change identity
         transform identity
         invalid-msg "Keine gültige Eingabe."
         as sa/FormInput}}]
  (let [field-state (ra/atom {:problems nil
                              :value ((:value-fn options))})
        get-value (fn []
                    (:value @field-state))
        apply-spec! (fn ([value]
                        (let [speced (spec/explain-data (:spec options) value)]
                          (swap! field-state assoc :problems
                                 (or
                                  (:cljs.spec.alpha/problems speced)
                                  (:cljs.spec/problems speced))))))
        change-fn (fn [e v]
                    (let [svalue (transform (.-value v))]
                      (on-change e v)
                      (apply-spec! svalue)
                      (swap! field-state assoc :value svalue)))
        opts (dissoc options :spec :as :invalid-msg :label)
        field-options (dissoc opts :on-change :value :placeholder)
        input-options (-> opts
                          (select-keys [:placeholder :value-fn])
                          (assoc :on-change change-fn))]
    {:key (or (:key options) (str (random-uuid)))
     :field-state field-state
     :as as
     :change-fn change-fn
     :spec spec
     :transform transform
     :get-value get-value
     :apply-spec! apply-spec!
     :invalid-msg invalid-msg
     :original-options options
     :field-options field-options
     :input-options input-options}))

(defn validated-input
  "A validated input element given a map from `make-validated-input`."
  ([input-obj]
   (validated-input input-obj nil))
  ([{:as obj :keys [field-state change-fn as invalid-msg
                    original-options field-options input-options]}
    children]
   (fn []
     [sa/FormField
      (assoc field-options :error (some? (:problems @field-state)))
      [:label (:label original-options)]
      [as (-> input-options
              (dissoc :value-fn)
              (assoc :value ((:value-fn input-options))))]
      (if-let [problems (:problems @field-state)]
        [sa/Message
         {:negative true}
         (if (fn? invalid-msg)
           (invalid-msg problems)
           invalid-msg)]
        [:div])
      children])))

(defn validated-input-elem
  "An input-element validated by the given spec on-change.
  Displays the given `invalid-msg` when the spec has problems,
  and passes `invalid-msg` the spec problems list if it is a function.
  Can transform the input value before it is passed to the spec using `transform`."
  [options]
  (let [item (make-validated-input options)]
    (validated-input item)))

(defn validated-form
  "A form that validates all the `validated-input` elements before
  allowing submission. Requires a list of `validated-inputs` with
  elements as created by `make-validated-input`, as well as a `submit-atom`
  which contains the function that the submit action on the form executes,
  so it can modify it to include validation."
  [{:as options
    :keys [as validated-inputs submit-atom]
    :or {as sa/Form
         validated-inputs []}}
   & children]
  (let [check-inputs! (fn []
                        (for [in validated-inputs]
                          (:problems
                           ((:apply-spec! in)
                            ((:get-value in))))))
        submit-fn @submit-atom
        wrap-submit (fn [& args]
                      (let [problems (check-inputs!)]
                        (if (every? nil? problems)
                          (apply submit-fn args)
                          (rf/dispatch [:submission/invalid]))))
        opts (dissoc options :as :validated-inputs :submit-atom)]
    (reset! submit-atom wrap-submit)
    (fn []
      [as opts children])))
