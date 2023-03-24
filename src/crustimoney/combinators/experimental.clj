(ns crustimoney.combinators.experimental
  "Experimental combinators. Anything can happen with them.

  These combinators do not have a string- or data-driven syntax (yet).
  To use them with those grammar syntaxes, you can use the
  `other-parsers` parameter of their respective `create-parser`
  functions, like:

      (require '[crustimoney.combinators.experimental :as e])

      (create-parser
        \"root= <- stream
         expr= <- '{' [0-9]+ '}'\"
        {:stream [::e/streaming handle-expr
                  [::e/recovering [:ref :expr] [:regex \".*?}\"]]})"
  (:refer-clojure :exclude [range])
  (:require [crustimoney.results :as r]))

(defn streaming
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

(defn recovering
  "Parse using `parser`. If it fails, try the `recovery` parser. If that
  succeeds, it results in a success node like this:

      [:crusti/recovered {:start .., :end .., :errors #{..}}]

  The errors are those of the first parser, and can be extracted using
  `success->recovered-errors`. If second parser fails, the result will
  be the errors of first parser. As with any parser, the name can be
  changed using `with-name`.

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
  [parser recovery]
  (with-meta
    (fn
      ([_text index]
       (r/->push parser index))

      ([text index result state]
       (if-let [errors (:errors state)]
         (if (r/success? result)
           (r/with-success-name :crusti/recovered
             (update result 1 assoc :errors errors))
           errors)
         (or (r/success? result)
             (r/->push recovery index {:errors result})))))
    {:recovering true}))

(defn success->recovered-errors
  "Returns the recovered errors from a result, as set by the
  `recovering` combinator parser."
  [success]
  (-> success second :errors))

(defn range
  "Like repeat, but the times the wrapped `parser` is matched must lie
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
