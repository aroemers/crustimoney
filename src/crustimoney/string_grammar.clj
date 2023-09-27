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
            [crustimoney.core :as core]
            [crustimoney.data-grammar :as data-grammar]
            [crustimoney.results :as r]))

;;; Value transformers

(defn- unescape-quotes [s]
  (str/replace s "\\'" "'"))

;;; Grammar definition

(def ^:private grammar
  (-> '{space #"[\s,]*"

        non-terminal=    #"[a-zA-Z_-]+"
        literal          ("'" > (:literal #"(\\'|[^'])*") "'")
        character-class= ("[" > #"(\\]|[^]])*" "]" #"[?*+]?")
        regex=           ("#" > literal)
        end-of-file=     "$"

        ref (non-terminal !"=" space !"<-")

        group-name (":" > (:group-name #"[a-zA-Z_-]+"))
        group=     ("(" > group-name ? space choice space ")")

        expr (ref / group / literal / character-class / end-of-file / regex)

        quantified ((:quantified expr (:operand #"[?+*]")) / expr)
        lookahead  ((:lookahead (:operand #"[&!]") > quantified) / quantified)

        cut= (">>" / ">")

        chain  ((:chain lookahead (space (cut / lookahead))+) / lookahead)
        choice ((:choice chain (space "/" space chain)+) / chain)

        rule= ((:rule-name non-terminal "="?) space "<-" >> space choice)
        root= ((:rules (space rule space)+) / (:no-rules space choice space) $)}
      data-grammar/create-parser
      core/compile))

;;; Parse result processing

(def ^:private transformations
  {:non-terminal    (r/coerce [s] [:ref {:to (keyword s)}])
   :literal         (r/coerce [s] [:literal {:text (unescape-quotes s)}])
   :character-class (r/coerce [s] [:regex {:pattern s}])
   :regex           (r/collect [[[_ {literal :text}]]] [:regex {:pattern literal}])
   :end-of-file     (r/coerce [_] [:eof])

   :group-name (r/coerce keyword)
   :group      (r/collect [[child1 child2]] (if child2 [:with-name {:key child1} child2] child1))

   :operand    (r/coerce {"!" :negate "&" :lookahead "?" :maybe "+" :repeat+ "*" :repeat*})
   :quantified (r/collect [[expr operand]] [operand expr])
   :lookahead  (r/collect [[operand expr]] [operand expr])

   :cut (r/coerce {">>" :hard-cut, ">" :soft-cut})

   :chain  (r/collect [children] (into [:chain] children))
   :choice (r/collect [children] (into [:choice] children))

   :rule-name (r/coerce keyword)
   :rule      (r/collect vec)
   :rules     (r/collect [rules] (into {} rules))

   :no-rules (r/collect first)
   :root     (r/collect first)})

(defn ^:no-doc vector-tree-for [success text]
  (r/transform success text transformations))

;;; Public namespace API

(defn create-parser
  "Create a parser (model) based on a string-based grammar definition.
  If the definition contains multiple rules, a map of parsers is
  returned.

  See the namespace documentation for the string format."
  [text]
  (let [result (-> (core/parse grammar text)
                   (r/errors->line-column text))]
    (if (set? result)
      (throw (ex-info "Failed to parse grammar" {:errors result}))
      (vector-tree-for result text))))
