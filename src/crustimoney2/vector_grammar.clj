(ns crustimoney2.vector-grammar
  "A basic vector-driven parser generator."
  (:require [crustimoney2.core :as core]
            [crustimoney2.combinators :as combinators]))

;;; Utility functions

(defn- map-kv [kf vf m]
  (reduce-kv (fn [a k v] (assoc a (kf k) (vf v))) {} m))

(defn ^:no-doc merge-other [tree other-parsers]
  (cond (and (map? tree) (map? other-parsers))
        (merge tree other-parsers)

        (map? other-parsers)
        (throw (IllegalArgumentException.
                "Supplying other parsers needs named rules in input grammar"))

        :otherwise
        tree))

;;; Parser creation

(defn- key-to-combinator [key]
  (case key
    :ref core/ref
    :eof (constantly combinators/eof)

    (ns-resolve (or (some-> key namespace symbol)
                    'crustimoney2.combinators)
                (symbol (name key)))))

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
  `crustimoney2.combinators` is assumed. Maps are walked as well,
  wrapped in `crustimoney2.core/rmap`. Other data is left as-is.

  This type of parser generator is not intended to be used directly,
  though you can. It is used as an intermediary format for other
  formats, such as the string-based and data-based grammars."
  [tree]
  (cond (map? tree)
        (core/rmap (map-kv identity create-parser tree))

        (vector? tree)
        (if-let [combinator (key-to-combinator (first tree))]
          (apply combinator (map create-parser (rest tree)))
          (throw (ex-info "combinator-key does not resolve" {:key (first tree)})))

        :otherwise
        tree))
