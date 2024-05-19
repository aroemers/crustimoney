(ns crustimoney.vector-grammar
  "A basic vector-driven parser generator. This type of parser generator
  is not intended to be used directly, though you can. It is used as
  an intermediary format for other formats, such as the string-based,
  data-based and combinator grammars.

  The format of the vector model is in hiccup-style. For example:

      [:chain [:literal {:text \"foo\"}] [:literal {:text \"bar\"}]]

  The first element in a vector is the name of the combinator. These
  are in direct relation to those in the `crustimoney.combinators`
  namespace. To use another namespace, simply use a namespaced
  keyword.

  The combinator implementation is called with at least a parameter
  map, and then any children it may have. So above model would lead to
  the following calls:

      (chain {} (literal {:text \"foo\"}) (literal {:text \"bar\"}))

  For recursive grammars, plain maps are used:

      {:root   [:ref {:to :foobar}]
       :foobar [literal {:text \"foobar\"}]}

  A map must have a `:root` entry, so the parser knows where to start.

  A rule's name key can be postfixed with `=`. The rule's parser is
  then wrapped with `with-name` (without the postfix). A `ref` to such
  rule is also without the postfix.

  Maps are processed inside a `with-scope`, binding all the `ref`
  calls. These maps can be nested, applying lexical scoping for the
  references.

  Anything other than a map of vector is left as-is."
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
