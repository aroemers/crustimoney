(ns ^:no-doc crustimoney.combinators.experimental
  "Experimental combinators"
  (:require [crustimoney.results :as r]))

(defn streaming
  "Experimental: like repeat*, but pushes results to callback function,
  instead of returning them as children.

  If callback is a symbol, it is resolved using `requiring-resolve`."
  [parser callback]
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
  "Experimental: try to turn errors from the wrapped parser into a
  success, by giving the callback a chance to find a new index.

  If callback is a symbol, it is resolved using `requiring-resolve`."
  [parser callback]
  (let [callback (cond-> callback (symbol? callback) requiring-resolve)]
    (fn
      ([_text index]
       (r/->push parser index))

      ([text index result _state]
       (if (r/success? result)
         result
         (if-let [end (callback text index result)]
           (r/->success index end)
           result))))))

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
