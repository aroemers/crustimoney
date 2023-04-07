(ns crustimoney.data-grammar
  "Create a parser based on a data grammar. The data is translated into
  combinators."
  (:require [crustimoney.vector-grammar :as vector-grammar]))

;;; Utility functions

(defn- map-kv [kf vf m]
  (reduce-kv (fn [a k v] (assoc a (kf k) (vf v))) {} m))

;;; Parser tree generator

(defprotocol DataGrammar
  (vector-tree [data]
    "Low-level protocol function which translates the data type
  into an intermediary vector-based representation. See
  `crustimoney.vector-grammar` for more on this format. This can be
  useful for debugging, or adding your own data type.

  In the latter case, add your type like so:

      (extend-type java.util.Date
        DataGrammar
        (vector-tree [date]
          [:my-namespace/my-flexible-date-parser date]))

  To see which data types are already supported, use `(->
  DataGrammar :impls keys)`"))

(extend-type Object
  DataGrammar
  (vector-tree [data]
    (throw (ex-info (str "Unknown data type: " (class data)) {:class (class data) :data data}))))

(extend-type clojure.lang.IPersistentMap
  DataGrammar
  (vector-tree [data]
    (map-kv (comp keyword name) vector-tree data)))

(defn- wrap-quantifiers [data]
  (->> data
       (reduce (fn [a e]
                 (condp = e
                   '* (conj (pop a) [:repeat* (vector-tree (last a))])
                   '+ (conj (pop a) [:repeat+ (vector-tree (last a))])
                   '? (conj (pop a) [:maybe (vector-tree (last a))])
                   (conj a e)))
               [])
       (apply list)))

(defn- wrap-lookahead [data]
  (->> (reverse data)
       (reduce (fn [a e]
                 (condp = e
                   '! (conj (rest a) [:negate (vector-tree (first a))])
                   '& (conj (rest a) [:lookahead (vector-tree (first a))])
                   (conj a e)))
               ())))

(extend-type clojure.lang.IPersistentVector
  DataGrammar
  (vector-tree [data] data))

(extend-type clojure.lang.IPersistentList
  DataGrammar
  (vector-tree [data]
    (if (keyword? (first data))
      [:with-name (first data) (vector-tree (apply list (rest data)))]
      (let [choices (->> data (partition-by #{'/}) (take-nth 2) (map (partial apply list)))]
        (if (= (count choices) 1)
          (let [wrapped (-> data wrap-quantifiers wrap-lookahead)]
            (if (= (count wrapped) 1)
              (vector-tree (first wrapped))
              (into [:chain] (map vector-tree wrapped))))
          (into [:choice] (map vector-tree choices)))))))

(extend-type clojure.lang.Symbol
  DataGrammar
  (vector-tree [data]
    (let [ref-name (str data)]
      (case ref-name
        "$"  [:eof]
        ">>" :hard-cut
        ">"  :soft-cut
        [:ref (keyword ref-name)]))))

(extend-type String
  DataGrammar
  (vector-tree [data]
    [:literal data]))

(extend-type java.util.regex.Pattern
  DataGrammar
  (vector-tree [data]
    [:regex data]))

(extend-type Character
  DataGrammar
  (vector-tree [data]
    [:literal (str data)]))

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
       regex-tag          #crusti/regex \"[a-z]\" ; EDN support
       eof                $

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
       combinator-call    [:with-error :fail #crusti/parser (\"fooba\" #\"r|z\")]
       custom-combinator  [:my.app/my-combinator ...]}

  To capture nodes in the parse result, you need to use named groups.
  If you postfix a rule name with `=`, the expression is automatically
  captured using the rule's name (without the postfix). Please read up
  on this at `crustimoney.combinators/grammar`.

  Keep in mind that `grammar` takes multiple maps, all of which can be
  referred to by the string grammar. For example:

      (grammar
       (create-parser '{root (\"Hello \" email)})
       {:email (regex #\"...\")})

  If you want to use an EDN grammar file or string, you can use
  `#crusti/regex` tagged literal for regular expressions. To read
  this, use the following:

      (clojure.edn/read-string {:readers *data-readers*} ...)"
  [data]
  (-> (vector-tree data)
      (vector-grammar/create-parser)))
