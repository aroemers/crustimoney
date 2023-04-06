(ns crustimoney.string-grammar
  "Create a parser based on a string grammar. The grammar is translated
  into combinators."
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
   {:space (c/regex #"\s*")

    :non-terminal= (c/regex "[a-zA-Z_-]+")

    :literal (c/chain (c/literal "'")
                      c/soft-cut
                      (c/with-name :literal
                        (c/regex #"(\\'|[^'])*"))
                      (c/literal "'"))

    :character-class= (c/chain (c/literal "[")
                               c/soft-cut
                               (c/regex #"(\\]|[^]])*")
                               (c/literal "]")
                               (c/regex #"[?*+]?"))

    :regex= (c/chain (c/literal "#")
                     c/soft-cut
                     (c/ref :literal))

    :end-of-file= (c/literal "$")

    :ref (c/chain (c/ref :non-terminal)
                  (c/negate (c/literal "="))
                  (c/ref :space)
                  (c/negate (c/literal "<-")))

    :cut= (c/choice (c/literal ">>") (c/literal ">"))

    :group-name (c/chain (c/literal ":")
                         c/soft-cut
                         (c/with-name :group-name
                           (c/regex "[a-zA-Z_-]+")))

    :group= (c/chain (c/literal "(")
                     c/soft-cut
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
                                    c/soft-cut
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
                    c/hard-cut
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

(defmulti ^:no-doc vector-tree-for
  (fn [_text node]
    (r/success->name node)))

(defmethod vector-tree-for :root
  [text node]
  (->> node r/success->children first (vector-tree-for text)))

(defmethod vector-tree-for :rules
  [text node]
  (into {} (map (partial vector-tree-for text) (r/success->children node))))

(defmethod vector-tree-for :no-rules
  [text node]
  (vector-tree-for text (first (r/success->children node))))

(defmethod vector-tree-for :rule
  [text node]
  (let [[name choice] (r/success->children node)
        rule-name     (keyword (r/success->text text name))]
    [rule-name (vector-tree-for text choice)]))

(defmethod vector-tree-for :non-terminal
  [text node]
  [:ref (keyword (r/success->text text node))])

(defmethod vector-tree-for :literal
  [text node]
  [:literal (unescape-quotes (r/success->text text node))])

(defmethod vector-tree-for :group
  [text node]
  (let [[child1 child2] (r/success->children node)]
    (if (= (r/success->name child1) :group-name)
      [:with-name (keyword (r/success->text text child1))
       (vector-tree-for text child2)]
      (vector-tree-for text child1))))

(defmethod vector-tree-for :character-class
  [text node]
  [:regex (r/success->text text node)])

(defmethod vector-tree-for :regex
  [text node]
  (let [literal (first (r/success->children node))]
    [:regex (unescape-quotes (r/success->text text literal))]))

(defmethod vector-tree-for :chain
  [text node]
  (into [:chain] (map (partial vector-tree-for text) (r/success->children node))))

(defmethod vector-tree-for :choice
  [text node]
  (into [:choice] (map (partial vector-tree-for text) (r/success->children node))))

(defmethod vector-tree-for :lookahead
  [text node]
  (let [[operand expr] (r/success->children node)
        parser         (vector-tree-for text expr)]
    (case (r/success->text text operand)
      "!" [:negate parser]
      "&" [:lookahead parser])))

(defmethod vector-tree-for :quantified
  [text node]
  (let [[expr operand] (r/success->children node)
        parser         (vector-tree-for text expr)]
    (case (r/success->text text operand)
      "?" [:maybe parser]
      "+" [:repeat+ parser]
      "*" [:repeat* parser])))

(defmethod vector-tree-for :end-of-file
  [_text _node]
  [:eof])

(defmethod vector-tree-for :cut
  [text node]
  ({">>" c/hard-cut, ">" c/soft-cut} (r/success->text text node)))

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
  The following definition describes the string grammar syntax in
  itself:

      space            <- [\\s]*

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

  A map of existing parsers can be supplied, which can be used by the
  string grammar. For example:

      (create-parser \"root <- 'Hello ' email\"
                     {:email (regex \"...\")})"
  ([text]
   (create-parser text nil))
  ([text other-parsers]
   (-> (vector-tree text)
       (vector-grammar/merge-other other-parsers)
       (vector-grammar/create-parser))))
