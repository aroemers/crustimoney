(ns crustimoney2.data-grammar
  "Create a parser based on a data grammar. The data is translated into
  combinators."
  (:require [crustimoney2.vector-grammar :as vector-grammar]))

;;; Utility functions

(defn- map-kv [kf vf m]
  (reduce-kv (fn [a k v] (assoc a (kf k) (vf v))) {} m))

;;; Parser tree generator

(defmulti vector-tree-for
  "Low-level (multi method) function which translates the data grammar
  into an intermediary vector-based representation. See
  `crustimoney2.vector-grammar` for more on this format. This can be
  useful for debugging, or adding your own data type.

  In the latter case, add your type like so:

      (defmethod vector-tree-for java.util.Date [date]
        [:my-namespace/my-flexible-date-parser date])

  To see which data types are already supported, use `(methods
  vector-tree-for)`"
  (fn [data]
    (type data)))

(defmethod vector-tree-for :default
  [data]
  (throw (ex-info (str "Unknown data type: " (class data)) {:class (class data) :data data})))

(defmethod vector-tree-for clojure.lang.IPersistentMap
  [data]
  (map-kv (comp keyword name) vector-tree-for data))

(defn- wrap-quantifiers [data]
  (->> data
       (reduce (fn [a e]
                 (condp = e
                   '* (conj (pop a) [:repeat* (last a)])
                   '+ (conj (pop a) [:repeat+ (last a)])
                   '? (conj (pop a) [:maybe (last a)])
                   (conj a e)))
               [])
       (apply list)))

(defn- wrap-lookahead [data]
  (->> (reverse data)
       (reduce (fn [a e]
                 (condp = e
                   '! (conj (rest a) [:negate (first a)])
                   '& (conj (rest a) [:lookahead (first a)])
                   (conj a e)))
               ())))

(defmethod vector-tree-for clojure.lang.IPersistentVector
  [data]
  (into [(first data)] (map vector-tree-for (rest data))))

(defmethod vector-tree-for clojure.lang.IPersistentList
  [data]
  (if (keyword? (first data))
    [:with-name (first data) (vector-tree-for (apply list (rest data)))]
    (let [choices (->> data (partition-by #{'/}) (take-nth 2) (map (partial apply list)))]
      (if (= (count choices) 1)
        (let [wrapped (-> data wrap-quantifiers wrap-lookahead)]
          (if (= (count wrapped) 1)
            (vector-tree-for (first wrapped))
            (into [:chain] (map vector-tree-for wrapped))))
        (into [:choice] (map vector-tree-for choices))))))

(defmethod vector-tree-for clojure.lang.Symbol
  [data]
  (let [ref-name (str data)]
    (case ref-name
      "$"  [:eof]
      "ε"  [:literal ""]
      ">>" :hard-cut
      ">"  :soft-cut
      [:ref (keyword ref-name)])))

(defmethod vector-tree-for java.lang.String
  [data]
  [:literal data])

(defmethod vector-tree-for java.util.regex.Pattern
  [data]
  [:regex data])

(defmethod vector-tree-for java.lang.Character
  [data]
  [:literal (str data)])

(deftype ^:no-doc Plain [data])

(defmethod vector-tree-for Plain
  [plain]
  (.data plain))

;;; Parser creation

(defn create-parser
  "Create a parser based on a data grammar definition. If a map with
  rules is supplied, a map of parsers is returned. Otherwise a single
  parser is returned. The following example shows what a data grammar
  looks like:

      {;; terminals
       literal            \"foo\"
       character          \\c
       regex              #\"[a-z]\"
       eof                $
       empty-string       ε

       ;; refs, chains, choices and grouping
       reference          literal
       chain              (literal regex)
       choices            (literal / regex / \"alice\" \"bob\")
       named-group        (:my-name literal / \"the end\" $)
       auto-named-group=  (literal / \"the end\" $)

       ;; quantifiers
       zero-to-many       (literal *)
       one-to-many        (\"bar\"+)
       zero-to-one        (\"foo\" \"bar\"?) ; bar is optional here

       ;; lookaheads
       lookahead          (& regex)
       negative-lookahead (!\"alice\")

       ;; cuts
       soft-cut           ('[' > expr? ']') ; note the >
       hard-cut           ((class-open class class-close >>)*) ; note the >>

       ;; direct combinator calls
       combinator-call    [:with-error #crust/plain :fail!
                           (\"fooba\" #\"r|z\")]
       custom-combinator  [:my.app/my-combinator literal]}

  Optionally an existing map of parsers can be supplied, which can
  refered to by the data grammar.

  To capture nodes in the parse result, you need to use named groups.
  If you postfix a rule name with `=`, the expression is automatically
  captured using the rule's name (without the postfix). Please read up
  on this at `crustimoney2.combinators/grammar`."
  ([data]
   (create-parser data nil))
  ([data other-parsers]
   (-> (vector-tree-for data)
       (vector-grammar/merge-other other-parsers)
       (vector-grammar/create-parser))))

(comment

  ;;; Eat your own dogfood, the string-grammar described in a data-grammar.

  (def superdogfood
    '{space #"\s*"

      non-terminal=    #"[a-zA-Z_-]+"
      literal          ("'" > (:literal ("\\'" / #"[^']")*) "'")
      character-class= ("[" ("\\]" / #"[^]]")* "]")
      special-char=    ("$" / "ε" / ".")
      ref              (non-terminal !"=" space !"<-")

      group-name (":" > (:group-name #"[a-zA-Z_-]+"))
      group=     ("(" > group-name ? space choice space ")")

      expr (ref / group / literal / character-class / special-char)

      quantified ((:quantified expr (:operand #"[?+*]")) / expr)
      lookahead  ((:lookahead (:operand #"[&!]") > quantified) / quantified)

      cut= (">>" / ">")

      chain  ((:chain lookahead (space (cut / lookahead))+) / lookahead)
      choice ((:choice chain (space "/" space chain)+) / chain)

      rule= ((:rule-name non-terminal "="?) space "<-" >> space choice)
      root= ((:rules (space rule space)+) / (:no-rules space choice space) $)})

  (def json
    '{root=    (space value space $)
      value    (string / number / boolean / array / null / object)
      string   ("\"" > (:string #"(\\\"|[^\"])*") "\"")
      number=  #"\d+"
      boolean= ("true" / "false")
      array=   ("[" > space (value (space "," space value)*)? space "]")
      null=    "null"
      object=  ("{" > space (entry (space "," space entry)*)? space "}" >>)
      entry=   (string space ":" space value)
      space    #"\s*"})

  (def jp (:root (create-parser json)))

  )
