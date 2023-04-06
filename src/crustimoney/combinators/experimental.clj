(ns crustimoney.combinators.experimental
  "Experimental combinators. These may get promoted, or changed,
  or dismissed.

  These combinators are not available in the string- or data-driven
  grammar (yet). To use them with those, combine them in a larger
  grammar like so:

      (require '[crustimoney.combinators.experimental :as e])

      (grammar
       (create-parser
         \"root= <- stream
          expr= <- '{' [0-9]+ '}'\")
       {:stream (e/stream handle-expr
                 (e/recover (ref :expr) (regex \".*?}\")))})"
  (:refer-clojure :exclude [range])
  (:require [crustimoney.results :as r]))

(defn stream
  "Like `repeat*`, but pushes results to the `callback` function,
  instead of returning them as children.

  If `callback` is a symbol, it is resolved using `requiring-resolve`."
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

(defn success->recovered-errors
  "Returns the recovered errors from a result, as set by the
  `recover` combinator parser."
  [success]
  (-> success second :errors))

(defn ^:no-doc with-success-recovered-errors
  "Sets the :errors attribute of a success."
  [errors success]
  (update success 1 assoc :errors errors))

(defn recover
  "Like a `choice`, capturing errors of the first choice, including
  soft-cuts in its scope.

  If the first `parser` fails, the `recovery` parser is tried. If it
  succeeds, it results in a success node like this:

      [:crusti/recovered {:start .., :end .., :errors #{..}}]

  The errors are those of the first parser, and can be extracted using
  `success->recovered-errors`. If recovery parser fails, the result
  will also be the errors of first parser.

  Example usage:

      (repeat* (recover (with-name :content
                          (chain (literal \"{\")
                                 (regex #\"\\d+\")
                                 (literal \"}\")))
                        (regex \".*?}\")))

  Parsing something like `{42}{nan}{100}` would result in:

      [nil {:start 0, :end 14}
       [:content {:start 0, :end 4}]
       [:crusti/recovered {:start 4, :end 9, :errors #{{:key :expected-match, :at 5, ...}}}]
       [:content {:start 9, :end 14}]]"
  [parser recovery]
  (with-meta
    (fn
      ([_text index]
       (r/->push parser index))

      ([_text index result state]
       (if-let [errors (:errors state)]
         ;; It was the result of the recovery parser
         (if (r/success? result)
           (r/with-success-name :crusti/recovered
             (with-success-recovered-errors errors
               result))
           errors)
         ;; It was the result of the first parser
         (or (r/success? result)
             (r/->push recovery index {:errors result})))))
    {:recovering true}))

(defn range
  "Like a repeat, but the times the wrapped `parser` is matched must lie
  within the given range. It will not try to parse more than `max`
  times."
  [parser min max]
  (assert (<= 0 min max) "min must at least be 0, and max must at least be min")

  (fn
    ([_text index]
     (if (< 0 max)
       (r/->push parser index [])
       (r/->success index index)))

    ([_text index result state]
     (if (r/success? result)
       (let [children (conj state result)]
         (if (= (count children) max)
           (r/->success index (-> children last r/success->end) children)
           (r/->push parser (r/success->end result) children)))
       (let [children state]
         (if (< (count children) min)
           result
           (let [end (or (some-> children last r/success->end) index)]
             (r/->success index end children))))))))
