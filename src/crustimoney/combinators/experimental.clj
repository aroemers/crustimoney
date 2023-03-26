(ns crustimoney.combinators.experimental
  "Experimental combinators. These may get promoted, or changed,
  or dismissed.

  These combinators are not available in the string- or data-driven
  grammar (yet). To use them with those, you can use the
  `other-parsers` parameter of their respective `create-parser`
  functions, like:

      (require '[crustimoney.combinators.experimental :as e])

      (create-parser
        \"root= <- stream
         expr= <- '{' [0-9]+ '}'\"
        {:stream [::e/stream handle-expr
                  [::e/recover [:ref :expr] [:regex \".*?}\"]]})

  Note that the other-parsers here is written in vector-grammar
  format. This is a little power-user trick, and allows you to declare
  `ref`s that do not have to resolve immediatly on their creation."
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
  [success errors]
  (update success 1 assoc :errors errors))

(defn recover
  "Like `choice`, capturing errors of the first choice, including
  soft-cuts in its scope.

  If the first `parser` fails, the `recoverers` parsers are tried in
  order. If one of those succeeds, it results in a success node like
  this:

      [:crusti/recovered {:start .., :end .., :errors #{..}}]

  The errors are those of the first parser, and can be extracted using
  `success->recovered-errors`. If all recovery parsers fail, the
  result will also be the errors of first parser.

  As with any parser, the name can be changed using `with-name`.

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
  [parser & recoverers]
  (with-meta
    (fn
      ([_text index]
       (r/->push parser index))

      ([text index result state]
       (if-let [errors (:errors state)]
         ;; It was the result of a recoverer
         (if (r/success? result)
           (r/with-success-name :crusti/recovered
             (with-success-recovered-errors result
               errors))
           (if-let [recoverer (nth recoverers (:pindex state) nil)]
             (r/->push recoverer index (update state :pindex inc))
             errors))
         ;; It was the result of the first parser
         (or (r/success? result)
             (when-let [recoverer (first recoverers)]
               (r/->push recoverer index {:errors result, :pindex 0}))
             result))))
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
       (let [children (update state conj result)]
         (if (= (count children) max)
           (r/->success index (-> children last r/success->end) children)
           (r/->push parser (r/success->end result) children)))
       (let [children state]
         (if (< (count children) min)
           result
           (let [end (or (some-> children last r/success->end) index)]
             (r/->success index end children))))))))
