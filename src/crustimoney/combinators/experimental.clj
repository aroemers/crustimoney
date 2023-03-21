(ns ^:no-doc crustimoney.combinators.experimental
  "Experimental combinators"
  (:refer-clojure :exclude [range])
  (:require [crustimoney.results :as r]))

(defn streaming
  "Experimental: like repeat*, but pushes results to callback function,
  instead of returning them as children.

  If callback is a symbol, it is resolved using `requiring-resolve`."
  [callback parser]
  (let [callback (cond-> callback (symbol? callback) requiring-resolve)]
    (fn
      ([_text index]
       (r/->push parser index {:end index}))

      ([_text index result state]
       (if (r/success? result)
         (let [end (r/success->end result)]
           (callback result)
           (r/->push parser end {:end end}))
         (r/->success index (:end state)))))))

(defn recovering
  "Experimental: like choice, also handling soft-cut. If second parser
  succeeds, the result node looks like:

      [:crusti/recovered {:start .., :end .., :errors #{..}}]

  The errors are those of the first parser. The name can be changed of
  course, by using `with-name`.

  If second parser fails, the errors of first parser are returned.

  Example usage:

      (repeat* (recovering
                (with-name :content
                  (chain (literal \"{\")
                         (regex #\"\\d+\")
                         (literal \"}\")))
                (regex \".*?}\"))

  Parsing something like `{42}{nan}{100}` would result in:

      [nil {:start 0, :end 14}
       [:content {:start 0, :end 4}]
       [:crusti/recovered {:start 4, :end 9, :errors #{{:key :expected-match, :at 5, ...}}}]
       [:content {:start 9, :end 14}]]"
  [parser recover]
  (with-meta
    (fn
      ([_text index]
       (r/->push parser index))

      ([text index result state]
       (if state
         (if (r/success? result)
           (r/with-success-name :crusti/recovered
             (update result 1 assoc :errors state))
           state)
         (or (r/success? result)
             (r/->push recover index result)))))
    {:recovering true}))

(defn success->recovered-errors
  "Returns the recovered errors from a result"
  [success]
  (-> success second :errors))

(defn range
  "Experimental: like repeat, but the times the wrapped parser is
  matched must lie within the given range. It will not try to parse
  more than max times."
  [parser min max]
  (assert (<= 0 min max) "min must at least be 0, and max must at least be min")

  (fn
    ([_text index]
     (if (< 0 max)
       (r/->push parser index [])
       (r/->success index index)))

    ([_text index result state]
     (if (r/success? result)
       (let [children (update state conj result)]
         (if (= (count children) max)
           (r/->success index (-> children last r/success->end) children)
           (r/->push parser (r/success->end result) children)))
       (let [children state]
         (if (< (count children) min)
           result
           (let [end (or (some-> children last r/success->end) index)]
             (r/->success index end children))))))))
