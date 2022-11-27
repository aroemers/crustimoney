(ns crustimoney2.string-grammar
  (:refer-clojure :exclude [ref])
  (:require [clojure.string :as str]
            [crustimoney2.core :as core :refer [ref]]
            [crustimoney2.combinators :refer :all]
            [crustimoney2.results :as r]))

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

(defmulti ^:no-doc parser-for
  (fn [node]
    (r/success->name node)))

(defmethod parser-for :root
  [node]
  (-> node r/success->children first parser-for))

(defmethod parser-for :rules
  [node]
  (into {} (map parser-for (r/success->children node))))

(defmethod parser-for :no-rules
  [node]
  {:root (parser-for (first (r/success->children node)))})

(defmethod parser-for :rule
  [node]
  (let [[child1 child2] (r/success->children node)
        rule-name       (keyword (r/success->attr child1 :value))]
    [rule-name (parser-for child2)]))

(defmethod parser-for :non-terminal
  [node]
  (core/ref (keyword (r/success->attr node :value))))

(defmethod parser-for :literal
  [node]
  (literal (r/success->attr node :value)))

(defmethod parser-for :group
  [node]
  (let [[child1 child2] (r/success->children node)]
    (if (= (r/success->name child1) :group-name)
      (with-name (keyword (r/success->attr child1 :value))
        (parser-for child2))
      (parser-for child1))))

(defmethod parser-for :character-class
  [node]
  (regex (r/success->attr node :value)))

(defmethod parser-for :chain
  [node]
  (apply chain (map parser-for (r/success->children node))))

(defmethod parser-for :choice
  [node]
  (apply choice (map parser-for (r/success->children node))))

(defmethod parser-for :lookahead
  [node]
  (let [[operand expr] (r/success->children node)
        parser         (parser-for expr)]
    (case (r/success->attr operand :value)
      "!" (negate parser)
      "&" (lookahead parser))))

(defmethod parser-for :quantified
  [node]
  (let [[expr operand] (r/success->children node)
        parser         (parser-for expr)]
    (case (r/success->attr operand :value)
      "?" (maybe parser)
      "+" (repeat+ parser)
      "*" (repeat* parser))))

(defmethod parser-for :end-of-file
  [_node]
  (eof (literal "")))

;;; Public namespace API

(defn create-parser
  "Create a parser based on a string-based grammar definition. If the
  definition contains multiple rules, a map of parsers is returned.
  Optionally an existing map of parsers can be supplied, which can be
  used by the string grammar. The following definition describes the
  string grammar synxtax in itself:

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

  To capture nodes in the parse result (i.e. the functionality of
  `combinators/with-name`), you need to use named groups."
  ([text]
   (create-parser text nil))
  ([text other-parsers]
   (let [result (core/parse (:root grammar) text)]
     (if (list? result)
       (throw (ex-info "Failed to parse grammar" {:errors (distinct result)}))
       (let [rmap (core/rmap (merge other-parsers (parser-for result)))]
         (cond-> rmap (= (count rmap) 1) (-> vals first)))))))

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
