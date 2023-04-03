(ns crustimoney.experimental.results
  "Expiremental result constructors, accessors and predicates.
  These may get promoted, or changed, or dismissed.")

(defn success->recovered-errors
  "Returns the recovered errors from a result, as set by the
  experimental `recover` combinator parser."
  [success]
  (-> success second :errors))

(defn ^:no-doc with-success-recovered-errors
  "Sets the `:errors` attribute of a success."
  [errors success]
  (update success 1 assoc :errors errors))
