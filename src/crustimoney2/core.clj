(ns crustimoney2.core
  "The main parsing functions."
  (:require [crustimoney2.caches :as caches]
            [crustimoney2.combinators :as c]
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
       [:child-1 {:start 0, :end 2}]
       [:child-2 {:start 2, :end 3}]]

  An error result looks like this:

      ({:key :failed-lookahead, :at 0}
       {:key :expected-literal, :at 0, :detail {:literal \"foo\"}})

  The parse function can take an options map, with the following
  options:

  - `:index`, the index at which to start parsing in the text, default 0.

  - `:cache`, the packrat cache to use, see the caches namespace.
  Default is treemap-cache. To disable caching, use nil.

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
         cache           (or (:cache opts (caches/treemap-cache)) caches/noop-cache)
         post-success    (if (:keep-nameless? opts) identity keep-named-children)
         infinite-check? (:infinite-check? opts true)]

     ;; Main parsing loop
     (loop [stack  [(r/->push parser start-index)]
            result nil
            state  nil
            cut-at 0]
       ;; See if there is something on the stack
       (if-let [stack-item (peek stack)]
         (let [parser (r/push->parser stack-item)
               index  (r/push->index stack-item)
               state' (r/push->state stack-item)
               ;; If there was previously a result, we are
               ;; backtracking. Otherwise, there was a push.
               result (if result
                        (parser text index result state)
                        (parser text index))]
           ;; Handle the parse result
           (cond
             ;; Handle a push
             (r/push? result)
             (let [push-parser (r/push->parser result)
                   push-index  (r/push->index result)
                   push-state  (r/push->state result)]
               ;; Check for infinite loops
               (when (and infinite-check? (infinite-loop? stack push-index push-parser))
                 (throw (ex-info "Infinite parsing loop detected" {:index push-index})))
               ;; Check if this parser on this index is already in the cache
               (if-let [hit (caches/fetch cache push-parser push-index)]
                 (recur stack hit push-state cut-at)
                 (recur (conj stack result) nil nil cut-at)))

             ;; Handle a success
             (r/success? result)
             (let [processed (post-success result)]
               ;; Check if it was a hard-cut success
               (if (-> result meta :hard-cut)
                 (do (caches/cut cache (r/success->end result))
                     (recur (pop stack) processed state' (r/success->end result)))
                 (do (caches/store cache parser index processed)
                     (recur (pop stack) processed state' cut-at))))

             ;; Handle a set of errors
             (set? result)
             (if-not (or (-> result meta :soft-cut) (< index cut-at))
               (recur (pop stack) result state' cut-at)
               result)

             ;; Something weird was returned from the parser
             :else
             (let [info {:parser parser, :type (type result)}]
               (throw (ex-info "Unexpected result from parser" info)))))
         result)))))

(comment

  (def combinator-grammar
    (c/grammar
     {:sum (c/choice (c/with-name :sum
                       (c/chain (c/ref :product)
                                (c/ref :sum-op)
                                :hard-cut
                                (c/ref :sum)))
                     (c/ref :product))

      :product (c/choice (c/with-name :product
                           (c/chain (c/ref :value)
                                    (c/ref :product-op)
                                    :hard-cut
                                    (c/ref :product)))
                         (c/ref :value))

      :value (c/choice (c/ref :number)
                       (c/chain (c/literal "(")
                                :soft-cut
                                (c/maybe (c/ref :sum))
                                (c/literal ")")))

      :sum-op (c/with-name :operation
                (c/regex #"(\+|-)"))

      :product-op (c/with-name :operation
                    (c/regex #"(\*|/)"))

      :number (c/with-name :number
                (c/regex #"[0-9]+"))}))

  (def string-grammar "
    sum        <- (:sum product sum-op >> sum) / product

    product    <- (:product value product-op >> product) / value

    value      <- number / '(' > sum? ')'

    sum-op     <- (:operation [+-])

    product-op <- (:operation [*/])

    number     <- (:number [0-9]+)")

  (def data-grammar
    '{sum ((:sum product sum-op >> sum) / product)

      product ((:product value product-op >> product) / value)

      value (number / "(" > sum ? ")")

      sum-op (:operation #"[+-]")

      product-op (:operation #"[*/]")

      number (:number #"[0-9]+")})

  )
