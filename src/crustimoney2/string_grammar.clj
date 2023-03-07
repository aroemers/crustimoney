(ns crustimoney2.string-grammar
  "Create a parser based on a string grammar. The grammar is translated
  into combinators."
  (:require [clojure.string :as str]
            [crustimoney2.combinators :as c]
            [crustimoney2.results :as r]
            [crustimoney2.vector-grammar :as vector-grammar]))

;;; Value transformers

(defn- unescape-quotes [s]
  (str/replace s "''" "'"))

(defn- unescape-brackets [s]
  (str/replace s "]]" "]"))

;;; Grammar definition

(def ^:private grammar
  (c/grammar
   {:space (c/regex "[ \t]*")

    :whitespace (c/regex #"\s*")

    :non-terminal (c/with-name :non-terminal
                    (c/with-value
                      (c/regex "[a-zA-Z_-]+")))

    :literal (c/chain (c/literal "'")
                      :soft-cut
                      (c/with-name :literal
                        (c/with-value unescape-quotes
                          (c/regex "(''|[^'])*")))
                      (c/literal "'"))

    :character-class (c/with-name :character-class
                       (c/with-value unescape-brackets
                         (c/regex #"\[(]]|[^]])*]")))

    :end-of-file (c/with-name :end-of-file
                   (c/literal "$"))

    :cut (c/with-name :cut
           (c/with-value {">>" :hard-cut, ">" :soft-cut}
             (c/choice (c/literal ">>") (c/literal ">"))))

    :group-name (c/chain (c/literal ":")
                         :soft-cut
                         (c/with-name :group-name
                           (c/with-value
                             (c/regex "[a-zA-Z_-]+"))))

    :group (c/with-name :group
             (c/chain (c/literal "(")
                      :soft-cut
                      (c/maybe (c/ref :group-name))
                      (c/ref :space)
                      (c/ref :choice)
                      (c/ref :space)
                      (c/literal ")")))

    :expr (c/choice (c/ref :non-terminal)
                    (c/ref :group)
                    (c/ref :literal)
                    (c/ref :character-class)
                    (c/ref :end-of-file))

    :quantified (c/choice (c/with-name :quantified
                            (c/chain (c/ref :expr)
                                     (c/with-name :operand
                                       (c/with-value
                                         (c/regex "[?+*]")))))
                          (c/ref :expr))

    :lookahead (c/choice (c/with-name :lookahead
                           (c/chain (c/with-name :operand
                                      (c/with-value
                                        (c/regex "[&!]")))
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

    :rule (c/with-name :rule
            (c/chain (c/with-name :rule-name
                       (c/ref :non-terminal))
                     (c/ref :space)
                     (c/literal "<-")
                     :hard-cut
                     (c/ref :space)
                     (c/ref :choice)))

    :root (c/with-name :root
            (c/chain (c/choice (c/with-name :rules
                                 (c/repeat+ (c/chain (c/ref :whitespace)
                                                     (c/ref :rule)
                                                     (c/ref :whitespace))))
                               (c/with-name :no-rules
                                 (c/chain (c/ref :whitespace)
                                          (c/ref :choice)
                                          (c/ref :whitespace))))
                     c/eof))}))

;;; Parse result processing

(defmulti ^:no-doc vector-tree-for
  (fn [node]
    (r/success->name node)))

(defmethod vector-tree-for :root
  [node]
  (-> node r/success->children first vector-tree-for))

(defmethod vector-tree-for :rules
  [node]
  (into {} (map vector-tree-for (r/success->children node))))

(defmethod vector-tree-for :no-rules
  [node]
  (vector-tree-for (first (r/success->children node))))

(defmethod vector-tree-for :rule
  [node]
  (let [[child1 child2] (r/success->children node)
        rule-name       (keyword (r/success->attr child1 :value))]
    [rule-name (vector-tree-for child2)]))

(defmethod vector-tree-for :non-terminal
  [node]
  [:ref (keyword (r/success->attr node :value))])

(defmethod vector-tree-for :literal
  [node]
  [:literal (r/success->attr node :value)])

(defmethod vector-tree-for :group
  [node]
  (let [[child1 child2] (r/success->children node)]
    (if (= (r/success->name child1) :group-name)
      [:with-name (keyword (r/success->attr child1 :value))
       (vector-tree-for child2)]
      (vector-tree-for child1))))

(defmethod vector-tree-for :character-class
  [node]
  [:regex (r/success->attr node :value)])

(defmethod vector-tree-for :chain
  [node]
  (into [:chain] (map vector-tree-for (r/success->children node))))

(defmethod vector-tree-for :choice
  [node]
  (into [:choice] (map vector-tree-for (r/success->children node))))

(defmethod vector-tree-for :lookahead
  [node]
  (let [[operand expr] (r/success->children node)
        parser         (vector-tree-for expr)]
    (case (r/success->attr operand :value)
      "!" [:negate parser]
      "&" [:lookahead parser])))

(defmethod vector-tree-for :quantified
  [node]
  (let [[expr operand] (r/success->children node)
        parser         (vector-tree-for expr)]
    (case (r/success->attr operand :value)
      "?" [:maybe parser]
      "+" [:repeat+ parser]
      "*" [:repeat* parser])))

(defmethod vector-tree-for :end-of-file
  [_node]
  [:eof])

(defmethod vector-tree-for :cut
  [node]
  (r/success->attr node :value))

;;; Public namespace API

(defn vector-tree
  "Low-level function which translates the string grammar into an
  intermediary vector-based representation. See
  `crustimoney2.vector-grammar` for more on this format. This can be
  useful for debugging."
  [text]
  (let [result (core/parse (:root grammar) text)]
    (if (set? result)
      (throw (ex-info "Failed to parse grammar" {:errors (r/errors->line-column result text)}))
      (vector-tree-for result))))

(defn create-parser
  "Create a parser based on a string-based grammar definition. If the
  definition contains multiple rules, a map of parsers is returned.
  Optionally an existing map of parsers can be supplied, which can be
  used by the string grammar. The following definition describes the
  string grammar syntax in itself:

      space           <- [ \t]*
      whitespace      <- [\\s]*

      non-terminal    <- (:non-terminal [a-zA-Z_-]+)
      literal         <- '''' > (:literal ('''''' / [^'])*) ''''
      character-class <- (:character-class '[' (']]' / [^]]])* ']')
      end-of-file     <- (:end-of-file '$')

      group-name      <- ':' > (:group-name [a-zA-Z_-]+)
      group           <- (:group '(' > group-name? space choice space ')')

      expr            <- non-terminal / group / literal / character-class / end-of-file

      quantified      <- (:quantified expr (:operand [?+*])) / expr
      lookahead       <- (:lookahead (:operand [&!]) > quantified) / quantified

      cut             <- (:hard-cut '>>') / (:soft-cut '>')

      chain           <- (:chain lookahead (space (cut / lookahead))+) / lookahead
      choice          <- (:choice chain (space '/' space chain)+) / chain

      rule            <- (:rule (:rule-name non-terminal) space '<-' >> space choice)
      rules           <- (:rules (whitespace rule whitespace)+)
      no-rules        <- (:no-rules whitespace choice whitespace)
      root            <- (:root rules / no-rules) $

  To capture nodes in the parse result, you need to use named groups."
  ([text]
   (create-parser text nil))
  ([text other-parsers]
   (-> (vector-tree text)
       (vector-grammar/merge-other other-parsers)
       (vector-grammar/create-parser))))

(comment

  ;;; I heard you like string grammars...

  ;;TODO: Other syntax for regexes? So not character class, but real regex? Or both?

  (def superdogfood "
    space           <- [ \t]*
    whitespace      <- [\\s]*

    non-terminal    <- (:non-terminal [a-zA-Z_-]+)
    literal         <- '''' > (:literal ('''''' / [^'])*) ''''
    character-class <- (:character-class '[' (']]' / [^]]])* ']')
    end-of-file     <- (:end-of-file '$')

    group-name      <- ':' > (:group-name [a-zA-Z_-]+)
    group           <- (:group '(' > group-name? space choice space ')')

    expr            <- non-terminal / group / literal / character-class / end-of-file

    quantified      <- (:quantified expr (:operand [?+*])) / expr
    lookahead       <- (:lookahead (:operand [&!]) > quantified) / quantified

    cut             <- (:hard-cut '>>') / (:soft-cut '>')

    chain           <- (:chain lookahead (space (cut / lookahead))+) / lookahead
    choice          <- (:choice chain (space '/' space chain)+) / chain

    rule            <- (:rule (:rule-name non-terminal) space '<-' >> space choice)
    root            <- (:root (:rules (whitespace rule whitespace)+) / (:no-rules whitespace choice whitespace)) $")

  (require '[instaparse.core :as insta])

  (def instafood "
    space           = #'[ \t]*'
    whitespace      = #'[\\s]*'

    non-terminal    = #'[a-zA-Z_-]+'
    literal         = \"'\" (\"''\" / #\"[^']\")* \"'\"
    character-class = '[' (']]' / #'[^]]')* ']'
    end-of-file     = '$'
    cut             = '>>' / '>'

    group-name      = ':' #'[a-zA-Z_-]+'
    group           = '(' group-name? space choice space ')'

    expr            = non-terminal / group / literal / character-class / end-of-file / cut

    quantified      = expr #'[?+*]' / expr
    lookahead       = #'[&!]' quantified / quantified

    chain           = lookahead (space lookahead)+ / lookahead
    choice          = chain (space '/' space chain)+ / chain

    rule            = non-terminal space '<-' space choice
    root            = (whitespace rule whitespace)+ / whitespace choice whitespace")

  )
