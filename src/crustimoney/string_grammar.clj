(ns crustimoney.string-grammar
  "Create a parser based on a string grammar. The grammar is translated
  into a parser (or map of parsers). The following definition
  describes the string grammar syntax in itself:

      space            <- [\\s,]*

      non-terminal=    <- [a-zA-Z_-]+
      literal          <- '\\'' > (:literal #'(\\\\\\'|[^\\'])*') '\\''
      character-class= <- '[' > #'(\\\\]|[^]])*' ']' [?*+]?
      regex=           <- '#' > literal
      ref              <- (non-terminal !'=' space !'<-')
      end-of-file=     <- '$'

      group-name       <- ':' > (:group-name [a-zA-Z_-]+)
      group=           <- '(' > group-name? space choice space ')'

      expr             <- ref / group / literal / character-class / end-of-file / regex

      quantified       <- (:quantified expr (:operand [?+*])) / expr
      lookahead        <- (:lookahead (:operand [&!]) > quantified) / quantified

      cut=             <- '>>' / '>'

      chain            <- (:chain lookahead (space (cut / lookahead))+) / lookahead
      choice           <- (:choice chain (space '/' space chain)+) / chain

      rule=            <- (:rule-name non-terminal '='?) space '<-' >> space choice
      root=            <- (:rules (space rule space)+) / (:no-rules space choice space) $

  To capture nodes in the parse result, you need to use named groups.
  If you postfix a rule name with `=`, the expression is automatically
  captured using the rule's name (without the postfix). Please read up
  on this at `crustimoney.combinators/grammar`.

  Keep in mind that `grammar` takes multiple maps, all of which can be
  referred to by the string grammar. For example:

      (grammar
       (create-parser \"root <- 'Hello ' email\")
       {:email (regex #\"...\")})"
  (:require [clojure.string :as str]
            [crustimoney.combinators :as c]
            [crustimoney.core :as core]
            [crustimoney.results :as r]
            [crustimoney.vector-grammar :as vector-grammar]))

;;; Value transformers

(defn- unescape-quotes [s]
  (str/replace s "\\'" "'"))

;;; Grammar definition

(def ^:private grammar
  (c/grammar
   {:space (c/regex #"[\s,]*")

    :non-terminal= (c/regex "[a-zA-Z_-]+")

    :literal (c/chain (c/literal "'")
                      :soft-cut
                      (c/with-name :literal
                        (c/regex #"(\\'|[^'])*"))
                      (c/literal "'"))

    :character-class= (c/chain (c/literal "[")
                               :soft-cut
                               (c/regex #"(\\]|[^]])*")
                               (c/literal "]")
                               (c/regex #"[?*+]?"))

    :regex= (c/chain (c/literal "#")
                     :soft-cut
                     (c/ref :literal))

    :end-of-file= (c/literal "$")

    :ref (c/chain (c/ref :non-terminal)
                  (c/negate (c/literal "="))
                  (c/ref :space)
                  (c/negate (c/literal "<-")))

    :cut= (c/choice (c/literal ">>") (c/literal ">"))

    :group-name (c/chain (c/literal ":")
                         :soft-cut
                         (c/with-name :group-name
                           (c/regex "[a-zA-Z_-]+")))

    :group= (c/chain (c/literal "(")
                     :soft-cut
                     (c/maybe (c/ref :group-name))
                     (c/ref :space)
                     (c/ref :choice)
                     (c/ref :space)
                     (c/literal ")"))

    :expr (c/choice (c/ref :ref)
                    (c/ref :group)
                    (c/ref :literal)
                    (c/ref :character-class)
                    (c/ref :end-of-file)
                    (c/ref :regex))

    :quantified (c/choice (c/with-name :quantified
                            (c/chain (c/ref :expr)
                                     (c/with-name :operand
                                       (c/regex "[?+*]"))))
                          (c/ref :expr))

    :lookahead (c/choice (c/with-name :lookahead
                           (c/chain (c/with-name :operand
                                      (c/regex "[&!]"))
                                    :soft-cut
                                    (c/ref :quantified)))
                         (c/ref :quantified))

    :chain (c/choice (c/with-name :chain
                       (c/chain (c/ref :lookahead)
                                (c/repeat+ (c/chain (c/ref :space)
                                                    (c/choice (c/ref :cut)
                                                              (c/ref :lookahead))))))
                     (c/ref :lookahead))

    :choice (c/choice (c/with-name :choice
                        (c/chain (c/ref :chain)
                                 (c/repeat+ (c/chain (c/ref :space)
                                                     (c/literal "/")
                                                     (c/ref :space)
                                                     (c/ref :chain)))))
                      (c/ref :chain))

    :rule= (c/chain (c/with-name :rule-name
                      (c/chain (c/ref :non-terminal)
                               (c/maybe (c/literal "="))))
                    (c/ref :space)
                    (c/literal "<-")
                    :hard-cut
                    (c/ref :space)
                    (c/ref :choice))

    :root= (c/chain (c/choice (c/with-name :rules
                                (c/repeat+ (c/chain (c/ref :space)
                                                    (c/ref :rule)
                                                    (c/ref :space))))
                              (c/with-name :no-rules
                                (c/chain (c/ref :space)
                                         (c/ref :choice)
                                         (c/ref :space))))
                    (c/eof))}))

;;; Parse result processing

(def ^:private transformations
  {:root (r/unite identity)

   :rules (r/unite [rules] (into {} rules))

   :no-rules (r/unite identity)

   :rule (r/unite vector)

   :rule-name (r/coerce keyword)

   :non-terminal (r/coerce [s] [:ref (keyword s)])

   :literal (r/coerce [s] [:literal (unescape-quotes s)])

   :group (r/unite [[child1 child2]] (if child2 [:with-name child1 child2] child1))

   :group-name (r/coerce keyword)

   :character-class (r/coerce [s] [:regex s])

   :regex (r/unite [[literal]] [:regex (second literal)])

   :chain (r/unite [children] (into [:chain] children))

   :choice (r/unite [children] (into [:choice] children))

   :lookahead (r/unite [[operand expr]] [operand expr])

   :quantified (r/unite [[expr operand]] [operand expr])

   :operand (r/coerce {"!" :negate "&" :lookahead "?" :maybe "+" :repeat+ "*" :repeat*})

   :end-of-file (r/coerce [s] [:eof])

   :cut (r/coerce {">>" :hard-cut, ">" :soft-cut})})

(defn ^:no-doc vector-tree-for [text success]
  (r/transform success text transformations))

;;; Public namespace API

(defn vector-tree
  "Low-level function which translates the string grammar into an
  intermediary vector-based representation. See
  `crustimoney.vector-grammar` for more on this format. This can be
  useful for debugging."
  [text]
  (let [result (core/parse (:root grammar) text)]
    (if (set? result)
      (throw (ex-info "Failed to parse grammar" {:errors (r/errors->line-column text result)}))
      (vector-tree-for text result))))

(defn create-parser
  "Create a parser based on a string-based grammar definition. If the
  definition contains multiple rules, a map of parsers is returned.

  See the namespace documentation for the string format."
  [text]
  (-> (vector-tree text)
      (vector-grammar/create-parser)))
