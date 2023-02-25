(ns crustimoney2.string-grammar
  "Create a parser based on a string grammar. The grammar is translated
  into combinators."
  (:refer-clojure :exclude [ref])
  (:require [clojure.string :as str]
            [crustimoney2.core :as core :refer [ref]]
            [crustimoney2.combinators :refer :all]
            [crustimoney2.results :as r]
            [crustimoney2.vector-grammar :as vector-grammar]))

;;; Value transformers

(defn- unescape-quotes [s]
  (str/replace s "''" "'"))

(defn- unescape-brackets [s]
  (str/replace s "]]" "]"))

;;; Grammar definition

(def ^:private grammar
  (core/rmap
   {:space (regex "[ \t]*")

    :whitespace (regex #"\s*")

    :non-terminal (with-name :non-terminal
                    (with-value
                      (regex "[a-zA-Z_-]+")))

    :literal (chain (literal "'")
                    (with-name :literal
                      (with-value unescape-quotes
                        (regex "(''|[^'])*")))
                    (literal "'"))

    :character-class (with-name :character-class
                       (with-value unescape-brackets
                         (regex #"\[(]]|[^]])*]")))

    :end-of-file (with-name :end-of-file
                   (literal "$"))

    :group-name (chain (literal ":")
                       (with-name :group-name
                         (with-value
                           (regex "[a-zA-Z_-]+"))))

    :group (with-name :group
             (chain (literal "(")
                    (maybe (ref :group-name))
                    (ref :space)
                    (ref :choice)
                    (ref :space)
                    (literal ")")))

    :expr (choice (ref :non-terminal)
                  (ref :group)
                  (ref :literal)
                  (ref :character-class)
                  (ref :end-of-file))

    :quantified (choice (with-name :quantified
                          (chain (ref :expr)
                                 (with-name :operand
                                   (with-value
                                     (regex "[?+*]")))))
                        (ref :expr))

    :lookahead (choice (with-name :lookahead
                         (chain (with-name :operand
                                  (with-value
                                    (regex "[&!]")))
                                (ref :quantified)))
                       (ref :quantified))

    :chain (choice (with-name :chain
                     (chain (ref :lookahead)
                            (repeat+ (chain (ref :space)
                                            (ref :lookahead)))))
                   (ref :lookahead))

    :choice (choice (with-name :choice
                      (chain (ref :chain)
                             (repeat+ (chain (ref :space)
                                             (literal "/")
                                             (ref :space)
                                             (ref :chain)))))
                    (ref :chain))

    :rule (with-name :rule
            (chain (with-name :rule-name
                     (ref :non-terminal))
                   (ref :space)
                   (literal "<-")
                   (ref :space)
                   (ref :choice)))

    :root (with-name :root
            (eof (choice (with-name :rules
                           (repeat+ (chain (ref :whitespace)
                                           (ref :rule)
                                           (ref :whitespace))))
                         (with-name :no-rules
                           (chain (ref :whitespace)
                                  (ref :choice)
                                  (ref :whitespace))))))}))

;;; Parse result processing

(defmulti ^:no-doc combinator-tree-for
  (fn [node]
    (r/success->name node)))

(defmethod combinator-tree-for :root
  [node]
  (-> node r/success->children first combinator-tree-for))

(defmethod combinator-tree-for :rules
  [node]
  (into {} (map combinator-tree-for (r/success->children node))))

(defmethod combinator-tree-for :no-rules
  [node]
  {:root (combinator-tree-for (first (r/success->children node)))})

(defmethod combinator-tree-for :rule
  [node]
  (let [[child1 child2] (r/success->children node)
        rule-name       (keyword (r/success->attr child1 :value))]
    [rule-name (combinator-tree-for child2)]))

(defmethod combinator-tree-for :non-terminal
  [node]
  [:ref (keyword (r/success->attr node :value))])

(defmethod combinator-tree-for :literal
  [node]
  [:literal (r/success->attr node :value)])

(defmethod combinator-tree-for :group
  [node]
  (let [[child1 child2] (r/success->children node)]
    (if (= (r/success->name child1) :group-name)
      [:with-name (keyword (r/success->attr child1 :value))
       (combinator-tree-for child2)]
      (combinator-tree-for child1))))

(defmethod combinator-tree-for :character-class
  [node]
  [:regex (r/success->attr node :value)])

(defmethod combinator-tree-for :chain
  [node]
  (into [:chain] (map combinator-tree-for (r/success->children node))))

(defmethod combinator-tree-for :choice
  [node]
  (into [:choice] (map combinator-tree-for (r/success->children node))))

(defmethod combinator-tree-for :lookahead
  [node]
  (let [[operand expr] (r/success->children node)
        parser         (combinator-tree-for expr)]
    (case (r/success->attr operand :value)
      "!" [:negate parser]
      "&" [:lookahead parser])))

(defmethod combinator-tree-for :quantified
  [node]
  (let [[expr operand] (r/success->children node)
        parser         (combinator-tree-for expr)]
    (case (r/success->attr operand :value)
      "?" [:maybe parser]
      "+" [:repeat+ parser]
      "*" [:repeat* parser])))

(defmethod combinator-tree-for :end-of-file
  [_node]
  [:eof])

;;; Public namespace API

(defn create-parser
  "Create a parser based on a string-based grammar definition. If the
  definition contains multiple rules, a map of parsers is returned.
  Optionally an existing map of parsers can be supplied, which can be
  used by the string grammar. The following definition describes the
  string grammar syntax in itself:

    space           <- [ \t]*
    whitespace      <- [\\s]*

    non-terminal    <- (:non-terminal [a-zA-Z_-]+)
    literal         <- '''' (:literal ('''''' / [^'])*) ''''
    character-class <- (:character-class '[' (']]' / [^]]])* ']')
    end-of-file     <- (:end-of-file '$')

    group-name      <- ':' (:group-name [a-zA-Z_-]+)
    group           <- (:group '(' group-name? space choice space ')')

    expr            <- non-terminal / group / literal / character-class / end-of-file

    quantified      <- (:quantified expr (:operand [?+*])) / expr
    lookahead       <- (:lookahead (:operand [&!]) quantified) / quantified

    chain           <- (:chain lookahead (space lookahead)+) / lookahead
    choice          <- (:choice chain (space '/' space chain)+) / chain

    rule            <- (:rule (:rule-name non-terminal) space '<-' space choice)
    rules           <- (:rules (whitespace rule whitespace)+)
    no-rules        <- (:no-rules whitespace choice whitespace)
    root            <- (:root rules / no-rules) $

  To capture nodes in the parse result, you need to use named groups."
  ([text]
   (create-parser text nil))
  ([text other-parsers]
   (let [result (core/parse (:root grammar) text)]
     (if (list? result)
       (throw (ex-info "Failed to parse grammar" {:errors (distinct result)}))
       (-> (combinator-tree-for result)
           (cond-> other-parsers (merge other-parsers))
           (vector-grammar/create-parser))))))

;;; I heard you like string grammars...

(def ^:private superdogfood "
  space           <- [ \t]*
  whitespace      <- [\\s]*

  non-terminal    <- (:non-terminal [a-zA-Z_-]+)
  literal         <- '''' (:literal ('''''' / [^'])*) ''''
  character-class <- (:character-class '[' (']]' / [^]]])* ']')
  end-of-file     <- (:end-of-file '$')

  group-name      <- ':' (:group-name [a-zA-Z_-]+)
  group           <- (:group '(' group-name? space choice space ')')

  expr            <- non-terminal / group / literal / character-class / end-of-file

  quantified      <- (:quantified expr (:operand [?+*])) / expr
  lookahead       <- (:lookahead (:operand [&!]) quantified) / quantified

  chain           <- (:chain lookahead (space lookahead)+) / lookahead
  choice          <- (:choice chain (space '/' space chain)+) / chain

  rule            <- (:rule (:rule-name non-terminal) space '<-' space choice)
  root            <- (:root (:rules (whitespace rule whitespace)+) / (:no-rules whitespace choice whitespace)) $")
