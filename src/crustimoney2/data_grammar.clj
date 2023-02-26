(ns crustimoney2.data-grammar
  "Create a parser based on a data grammar. The data is translated into
  combinators."
  (:require [crustimoney2.core :as core]
            [crustimoney2.vector-grammar :as vector-grammar]))

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
      "$" [:eof]
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
  parser is returned. Optionally an existing map of parsers can be
  supplied, which can refered to by the data grammar. The following
  example shows what a data grammar looks like:

    {;; terminals
     literal            \"foo\"
     character          \\c
     regex              #\"[a-z]\"
     eof                $

     ;; refs and grouping
     reference          literal
     chain              (literal regex)
     choices            (literal / regex / \"alice\" \"bob\")
     named-group        (:my-name literal / \"the end\" $)

     ;; quantifiers
     zero-to-many       (literal *)
     one-to-many        (\"bar\"+)
     zero-to-one        (\"foo\" \"bar\"?) ; bar is optional here

     ;; lookaheads
     lookahead          (& regex)
     negative-lookahead (!\"alice\")

     ;; direct combinator calls
     combinator-call       [:with-value (:bax \"bar\" / \"baz\")]
     combinator-plain-data [:with-error #crust/plain :fail! \"foo\"]
     custom-combinator     [:my.app/my-combinator literal]}

  To capture nodes in the parse result, you need to use named groups."
  ([data]
   (create-parser data nil))
  ([data other-parsers]
   (assert (or (nil? other-parsers) (map? data))
           "data must be a map when supplying other parsers")
   (-> (vector-tree-for data)
       (cond-> other-parsers (merge other-parsers))
       (vector-grammar/create-parser))))

;;; Eat your own dogfood

(def ^:private superdogfood
  '{space      #"[ \t]*"
    whitespace #"\s*"

    non-terminal    (:non-terminal #"[a-zA-Z_-]+")
    literal         ("'" (:literal ("''" / #"[^']")*) "'")
    character-class (:character-class "[" ("]]" / #"[^]]")* "]")
    end-of-file     (:end-of-file "$")

    group-name (":" (:group-name #"[a-zA-Z_-]+"))
    group      (:group "(" group-name ? space choice space ")")

    expr (non-terminal / group / literal / character-class / end-of-file)

    quantified ((:quantified expr (:operand #"[?+*]")) / expr)
    lookahead  ((:lookahead (:operand #"[&!]") quantified) / quantified)

    chain  ((:chain lookahead (space lookahead)+) / lookahead)
    choice ((:choice chain (space "/" space chain)+) / chain)

    rule (:rule (:rule-name non-terminal) space "<-" space choice)
    root ((:root (:rules (whitespace rule whitespace)+) / (:no-rules whitespace choice whitespace)) $)})
