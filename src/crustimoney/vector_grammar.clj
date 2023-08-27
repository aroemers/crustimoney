(ns crustimoney.vector-grammar
  "A basic vector-driven parser generator. This type of parser generator
  is not intended to be used directly, though you can. It is used as
  an intermediary format for other formats, such as the string-based
  and data-based grammars.

  A rule's name key can be postfixed with `=`. The rule's parser is
  then wrapped with `with-name` (without the postfix). A `ref` to such
  rule is also without the postfix.

  However, it is encouraged to be very intentional about which nodes
  should be captured and when. For example, the following (string)
  grammar ensures that the `:prefixed` node is only in the result when
  applicable.

      root=    <- prefixed (' ' prefixed)*
      prefixed <- (:prefixed '!' body) / body
      body=    <- [a-z]+

  Parsing \"foo !bar\" would result in the following result tree:

      [:root {:start 0, :end 8}
       [:body {:start 0, :end 3}]
       [:prefixed {:start 4, :end 8}
        [:body {:start 5, :end 8}]]]"
  (:refer-clojure :exclude [compile])
  (:require [clojure.string :as str]))

;;; Auto-capture mechanics

(def ^:private auto-capture-re #"=$")

(defn- auto-capture [grammar]
  (reduce-kv (fn [a k v]
               (let [rule-name     (name k)
                     auto-capture? (re-find auto-capture-re rule-name)
                     rule-key      (keyword (str/replace rule-name auto-capture-re ""))
                     rule-expr     (cond->> v auto-capture? (conj [:with-name {:key rule-key}]))]
                 (assoc a rule-key rule-expr)))
             {} grammar))


;;; Compiling vector model

(defn- keyword-to-combinator [key]
  (requiring-resolve (symbol (or (namespace key) "crustimoney.combinators")
                             (name key))))

(defn- compile-scoped [scope parser]
  ((fn inner-compile [parser]
     (cond (map? parser)
           (let [new-scope (atom nil)
                 compiled  (-> (auto-capture parser)
                               (update-vals (partial compile-scoped new-scope)))
                 grammar   (swap! new-scope merge compiled)]
             (if-let [unknown-refs (seq (remove grammar (keys grammar)))]
               (throw (ex-info "Detected unknown keys in refs" {:unknown-keys unknown-refs}))
               (or (:root compiled) (throw (ex-info "Missing :root rule in grammar" {})))))

           (vector? parser)
           (let [[key & more]    parser
                 [args children] (if (map? (first more))
                                   [(first more) (rest more)]
                                   [{} more])
                 combinator      (keyword-to-combinator key)]
             (if combinator
               (apply combinator (with-meta args {:scope scope}) (map inner-compile children))
               (throw (ex-info (str "Could not resolve combinator key " key) {:combinator key}))))

           :else parser))
   parser))


;;; Parser creation

(defn compile
  "Create a (compiled) parser based on a vector-driven combinator model.
  For example:

      {:root= [:chain [:ref {:to :foo}] [:ref {:to :bar}]]
       :foo     [:literal {:text \"foo\"}]
       :bar     [:with-name {:key :bax}
                 [:choice [:literal {:text \"bar\"}]
                          [:literal {:text \"baz\"}]]]}

  Each vector yields a combinator invocation, referenced
  by the first keyword. If the keyword does not have a namespace,
  `crustimoney.combinators` is assumed.

  Maps are walked as well, applying auto-captures and processing all
  values. A map must have a `:root` entry.

  Other data is left as-is, including compiled parser functions."
  [model]
  (compile-scoped nil model))
