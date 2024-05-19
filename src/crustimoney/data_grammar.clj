(ns crustimoney.data-grammar
  "Create a parser (model) based on a data grammar. The data is
  translated into a single parser or map of parsers. The following
  example shows what a data grammar looks like:

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
       combinator-call    [:with-error {:key :fail} #crusti/parser (\"fooba\" #\"r|z\")]
       custom-combinator  [:my.app/my-combinator ...]}

  To capture nodes in the parse result, you need to use named groups.
  If you postfix a rule name with `=`, the expression is automatically
  captured using the rule's name (without the postfix).

  Keep in mind that `create-parser` returns a parser model, which can
  be combined with other grammars. For example:

      (merge
       (create-parser '{root (\"Hello \" email)})
       {:email (regex #\"...\")})

  If you want to use an EDN format, you can use `#crusti/regex` tagged
  literal for regular expressions. To be able to read this, use the
  following:

      (clojure.edn/read-string {:readers *data-readers*} ...)")

;;; Utility functions

(defn- map-kv [kf vf m]
  (reduce-kv (fn [a k v] (assoc a (kf k) (vf v))) {} m))

;;; Parser tree generator

(defprotocol DataGrammar
  (vector-model [data]
    "Low-level protocol function which translates the data type into a
  vector-based model. See `crustimoney.vector-grammar` for more on
  this format. This can be useful for debugging, or adding your own
  data type.

  In the latter case, add your type like so:

      (extend-type java.util.Date
        DataGrammar
        (vector-model [date]
          [:my-namespace/my-flexible-date-parser {:date date}]))

  To see which data types are already supported, use `(->
  DataGrammar :impls keys)`"))

(extend-type Object
  DataGrammar
  (vector-model [data]
    (throw (ex-info (str "Unknown data type: " (class data)) {:class (class data) :data data}))))

(extend-type clojure.lang.IPersistentMap
  DataGrammar
  (vector-model [data]
    (map-kv (comp keyword name) vector-model data)))

(defn- wrap-quantifiers [data]
  (->> data
       (reduce (fn [a e]
                 (condp = e
                   '* (conj (pop a) [:repeat* (vector-model (last a))])
                   '+ (conj (pop a) [:repeat+ (vector-model (last a))])
                   '? (conj (pop a) [:maybe (vector-model (last a))])
                   (conj a e)))
               [])
       (apply list)))

(defn- wrap-lookahead [data]
  (->> (reverse data)
       (reduce (fn [a e]
                 (condp = e
                   '! (conj (rest a) [:negate (vector-model (first a))])
                   '& (conj (rest a) [:lookahead (vector-model (first a))])
                   (conj a e)))
               ())))

(extend-type clojure.lang.IPersistentVector
  DataGrammar
  (vector-model [data] data))

(extend-type clojure.lang.IPersistentList
  DataGrammar
  (vector-model [data]
    (if (keyword? (first data))
      [:with-name {:key(first data)} (vector-model (apply list (rest data)))]
      (let [choices (->> data (partition-by #{'/}) (take-nth 2) (map (partial apply list)))]
        (if (= (count choices) 1)
          (let [wrapped (-> data wrap-quantifiers wrap-lookahead)]
            (if (= (count wrapped) 1)
              (vector-model (first wrapped))
              (into [:chain] (map vector-model wrapped))))
          (into [:choice] (map vector-model choices)))))))

(extend-type clojure.lang.Symbol
  DataGrammar
  (vector-model [data]
    (let [ref-name (str data)]
      (case ref-name
        "$"  [:eof]
        ">>" :hard-cut
        ">"  :soft-cut
        [:ref {:to (keyword ref-name)}]))))

(extend-type String
  DataGrammar
  (vector-model [data]
    [:literal {:text data}]))

(extend-type java.util.regex.Pattern
  DataGrammar
  (vector-model [data]
    [:regex {:pattern data}]))

(extend-type Character
  DataGrammar
  (vector-model [data]
    [:literal {:text (str data)}]))

;;; Parser creation

(defn create-parser
  "Create a parser (model) based on a data-grammar definition. If a map
  with rules is supplied, a map is returned. Otherwise a single parser
  is returned.

  See namespace documentation for the data-grammar format."
  [data]
  (vector-model data))
