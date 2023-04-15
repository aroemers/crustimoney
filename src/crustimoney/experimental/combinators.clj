(ns crustimoney.experimental.combinators
  "Experimental combinators. These may get promoted, or changed,
  or dismissed.

  These combinators are not available in the string- or data-driven
  grammar (yet). To use them with those, combine them in a larger
  grammar like so:

      (require '[crustimoney.experimental.combinators :as e])

      (grammar
       (create-parser
         \"root= <- stream
          expr= <- '{' [0-9]+ '}'\")
       {:stream (e/stream*
                 (chain
                   (e/with-callback handle-expr
                     (e/recover (ref :expr) (regex \".*?}\")))
                   :hard-cut))})

  Above example also shows how to setup a streaming parser. The
  `stream*` ensures that the result tree does not grow, the
  `with-callback` passes chunks of results to a function, before it is
  hard-cut. The hard-cut ensures both the packrat cache and the
  streaming reader buffer (if used, see `reader` namespace) are kept
  small."
  (:refer-clojure :exclude [range])
  (:require [crustimoney.experimental.results :as er]
            [crustimoney.results :as r]))

(defn with-callback
  "Pushes (success) result of `parser` to the 2-arity `callback`
  function. The callback receives the text and the success result.

  If `callback` is a symbol, it is resolved using `requiring-resolve`."
  [callback parser]
  (let [callback (cond-> callback (symbol? callback) requiring-resolve)]
    (fn [text & args]
      (let [result (apply parser text args)]
        (when (r/success? result)
          (callback text result))
        result))))

(defn stream*
  "Like `repeat*`, but does does not keep its children. Can be used in
  combination with `with-callback` combinator, for example."
  [parser]
  (fn
    ([_text index]
     (r/->push parser index {:end index}))

    ([_text index result state]
     (if (r/success? result)
       (let [end (r/success->end result)]
         (r/->push parser end {:end end}))
       (r/->success index (:end state))))))

(defn stream+
  "Like `repeat+`, but does does not keep its children. Can be used in
  combination with `with-callback` combinator, for example."
  [parser]
  (fn
    ([_text index]
     (r/->push parser index {:end nil}))

    ([_text index result state]
     (if (r/success? result)
       (let [end (r/success->end result)]
         (r/->push parser end {:end end}))
       (if-let [end (:end state)]
         (r/->success index end)
         result)))))

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
             (er/with-success-recovered-errors errors
               result))
           errors)
         ;; It was the result of the first parser
         (or (r/success? result)
             (r/->push recovery index {:errors result})))))
    {:recovering true}))

(defn range
  "Like a repeat, but the times the wrapped `parser` is matched must lie
  within the given range. It will not try to parse more than `max`
  times. If `max` is not supplied, there is no maximum."
  ([parser min]
   (range parser min Long/MAX_VALUE))
  ([parser min max]
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
              (r/->success index end children)))))))))
