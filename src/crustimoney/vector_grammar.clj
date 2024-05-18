(ns crustimoney.vector-grammar
  "A basic vector-driven parser generator. This type of parser generator
  is not intended to be used directly, though you can. It is used as
  an intermediary format for other formats, such as the string-based
  and data-based grammars.

  A rule's name key can be postfixed with `=`. The rule's parser is
  then wrapped with `with-name` (without the postfix). A `ref` to such
  rule is also without the postfix.

  However, it is encouraged to be very intentional about which nodes
  should be captured and when. For example, the following (string)
  grammar ensures that the `:prefixed` node is only in the result when
  applicable.

      root=    <- prefixed (' ' prefixed)*
      prefixed <- (:prefixed '!' body) / body
      body=    <- [a-z]+

  Parsing \"foo !bar\" would result in the following result tree:

      [:root {:start 0, :end 8}
       [:body {:start 0, :end 3}]
       [:prefixed {:start 4, :end 8}
        [:body {:start 5, :end 8}]]]"
  (:refer-clojure :exclude [compile])
  (:require [crustimoney.combinators :as combinators]))

;;; Utilities

(defn- keyword-to-combinator [key]
  (requiring-resolve (symbol (or (namespace key) "crustimoney.combinators")
                             (name key))))

;;; Parser creation

(defn compile
  "Create a (compiled) parser based on a vector-driven combinator model.
  For example:

      {:root= [:chain [:ref {:to :foo}] [:ref {:to :bar}]]
       :foo   [:literal {:text \"foo\"}]
       :bar   [:with-name {:key :bax}
               [:choice [:literal {:text \"bar\"}]
                        [:literal {:text \"baz\"}]]]}

  Each vector yields a combinator invocation, referenced
  by the first keyword. If the keyword does not have a namespace,
  `crustimoney.combinators` is assumed.

  Maps are walked as well (using `with-scope`), applying auto-captures
  and processing all values. A map must have a `:root` entry.

  Other data is left as-is, including compiled parser functions."
  [model]
  (cond (map? model)
        (let [compiled (combinators/with-scope
                         (update-vals model compile))]
          (or (:root compiled) (throw (ex-info "Missing :root rule in grammar" {}))))

        (vector? model)
        (let [[key & more]    model
              [args children] (if (map? (first more))
                                [(first more) (rest more)]
                                [{} more])
              combinator      (keyword-to-combinator key)]
          (if combinator
            (apply combinator args (map compile children))
            (throw (ex-info (str "Could not resolve combinator key " key) {:combinator key}))))

        :else model))
