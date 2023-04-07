(ns crustimoney.vector-grammar
  "A basic vector-driven parser generator."
  (:require [crustimoney.combinators :as c]))

;;; Parser creation

(defn- key-to-combinator [key]
  (requiring-resolve (symbol (or (some-> key namespace)
                                 "crustimoney.combinators")
                             (name key))))

(defn create-parser
  "Create a parser based on a vector-driven combinator tree. For
  example:

      {:foobar [:chain [:ref :foo] [:ref :bar]]
       :foo    [:literal \"foo\"]
       :bar    [:with-name :bax
                [:choice [:literal \"bar\"]
                         [:literal \"baz\"]]]}

  Each vector is expanded into the combinator invocation, referenced
  by the first keyword. If the keyword does not have a namespace,
  `crustimoney.combinators` is assumed. Maps are walked as well,
  wrapped in `crustimoney.combinators/grammar`. Other data is left
  as-is.

  This type of parser generator is not intended to be used directly,
  though you can. It is used as an intermediary format for other
  formats, such as the string-based and data-based grammars."
  [tree]
  (cond (map? tree)
        (c/grammar (update-vals tree create-parser))

        (vector? tree)
        (if-let [combinator (key-to-combinator (first tree))]
          (apply combinator (map create-parser (rest tree)))
          (throw (ex-info "combinator-key does not resolve" {:key (first tree)})))

        :else
        tree))
