(ns crustimoney2.core
  "The main parsing functions."
  (:refer-clojure :exclude [ref])
  (:require [crustimoney2.caches :as caches]
            [crustimoney2.combinators :refer [literal regex chain choice with-name with-value]]
            [crustimoney2.results :as r]))

;;; Internals

(defn- keep-named-children
  "Process a success result by keeping only the named children, merged
  with the named children of nameless children. If that makes sense."
  [success]
  (let [children (mapcat (fn [child]
                           (if (r/success->name child)
                             [child]
                             (filter r/success->name (r/success->children child))))
                         (r/success->children success))]
    (r/with-success-children success children)))

(defn- infinite-loop? [stack index parser]
  (loop [i (dec (count stack))]
    (when (<= 0 i)
      (let [stack-item (nth stack i)]
        (when (= (r/push->index stack-item) index)
          (if (= (r/push->parser stack-item) parser)
            true
            (recur (dec i))))))))

;;; Parsing virtual machine

(defn parse
  "Use the given parser to parse the supplied text string. The result
  will either be a success (a hiccup-style vector) or a set of
  errors. By default only named nodes are kept in a success
  result (the root node is allowed to be nameless).

  A success result looks like this:

      [:name {:start 0, :end 3}
       [:child-1 {:start 0, :end 2, :value \"aa\"}]
       [:child-2 {:start 2, :end 3}]]

  An error result looks like this:

      ({:key :failed-lookahead, :at 0}
       {:key :expected-literal, :at 0, :detail {:literal \"foo\"}})

  The parse function can take an options map, with the following
  options:

  - `:index`, the index at which to start parsing in the text, default 0.

  - `:cache`, the packrat cache to use, see the caches namespace.
  Default is basic-cache. To disable caching, use nil.

  - `:infinite-check?`, check for infinite loops during parsing.
  Default is true. Setting it to false yields a small performance
  boost.

  - `:keep-nameless?`, set this to true if nameless success nodes
  should be kept in the parse result. This can be useful for
  debugging. Defaults to false."
  ([parser text]
   (parse parser text nil))
  ([parser text opts]
   ;; Options parsing
   (let [start-index     (:index opts 0)
         cache           (or (:cache opts (caches/basic-cache)) caches/noop-cache)
         post-success    (if (:keep-nameless? opts) identity keep-named-children)
         infinite-check? (:infinite-check? opts true)]

     ;; Main parsing loop
     (loop [stack  [(r/->push parser start-index)]
            result nil
            state  nil]
       (if-let [stack-item (peek stack)]
         (let [parser (r/push->parser stack-item)
               index  (r/push->index stack-item)
               state' (r/push->state stack-item)
               result (or (if result
                            (parser text index result state)
                            (parser text index))
                          ())]
           (cond (r/push? result)
                 (let [push-parser (r/push->parser result)
                       push-index  (r/push->index result)
                       push-state  (r/push->state result)]
                   (when (and infinite-check? (infinite-loop? stack push-index push-parser))
                     (throw (ex-info "Infinite parsing loop detected" {:index push-index})))
                   (if-let [hit (caches/fetch cache push-parser push-index)]
                     (recur stack hit push-state)
                     (recur (conj stack result) nil nil)))

                 (r/success? result)
                 (let [processed (post-success result)]
                   (caches/store cache parser index processed)
                   (recur (pop stack) processed state'))

                 (set? result)
                 (recur (pop stack) result state')

                 :else
                 (let [info {:parser parser, :type (type result)}]
                   (throw (ex-info "Unexpected result from parser" info)))))
         result)))))

;;; Recursive grammar definition

(def ^:dynamic ^:no-doc *parsers*)

(defn ref
  "Creates a parser function that wraps another parser function, which
  is referred to by the given key. Needs to be called within the
  lexical scope of `rmap`."
  [key]
  (assert (bound? #'*parsers*)
    "Cannot use ref function outside rmap macro")
  (swap! *parsers* assoc key nil)
  (let [parsers *parsers*]
    (fn
      ([_ index]
       (r/->push (get @parsers key) index))
      ([_ _ result _]
       result))))

(defn ^:no-doc rmap* [f]
  (binding [*parsers* (atom nil)]
    (let [result (swap! *parsers* merge (f))]
      (if-let [unknown-refs (seq (remove result (keys result)))]
        (throw (ex-info "Detected unknown keys in refs" {:unknown-keys unknown-refs}))
        result))))

(defmacro rmap
  "Takes (something that evaluates to) a map, in which the entries can
  refer to each other using the `ref` function. In other words, a
  recursive map. For example:

      (rmap {:foo  (literal \"foo\")
             :root (chain (ref :foo) \"bar\")})"
  [grammar]
  `(rmap* (fn [] ~grammar)))


(comment

  (def combinator-grammar
    (rmap
     {:sum (choice (with-name :sum
                     (chain (ref :product)
                            (ref :sum-op)
                            (ref :sum)))
                   (ref :product))

      :product (choice (with-name :product
                         (chain (ref :value)
                                (ref :product-op)
                                (ref :product)))
                       (ref :value))

      :value (choice (ref :number)
                     (chain (literal "(")
                            (ref :sum)
                            (literal ")")))

      :sum-op (with-name :operation
                (regex #"(\+|-)"))

      :product-op (with-name :operation
                    (regex #"(\*|/)"))

      :number (with-name :number
                (regex #"[0-9]+"))}))

  (def string-grammar "
    sum        <- (:sum product sum-op sum) / product

    product    <- (:product value product-op product) / value

    value      <- number / '(' sum ')'

    sum-op     <- (:operation [+-])

    product-op <- (:operation [*/])

    number     <- (:number [0-9]+)")

  (def data-grammar
    '{sum ((:sum product sum-op sum) / product)

      product ((:product value product-op product) / value)

      value (number / "(" sum ")")

      sum-op (:operation #"[+-]")

      product-op (:operation #"[*/]")

      number (:number #"[0-9]+")})

  )
