(ns crustimoney.core-new
  (:refer-clojure :exclude [compile])
  (:require [crustimoney.core :as core]))

(defn- keyword-to-combinator [key]
  (requiring-resolve (symbol (or (namespace key) "crustimoney.combinators-new")
                             (name key))))

(defn- compile-scoped [scope parser]
  ((fn inner-compile [parser]
     (cond (map? parser)
           (let [new-scope (atom nil)
                 compiled  (update-vals parser (partial compile-scoped new-scope))
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
             (apply combinator (with-meta args {:scope scope}) (map inner-compile children)))

           :else parser))
   parser))

(defn compile [parser]
  (compile-scoped nil parser))

(defn parse [parser text]
  (let [compiled (compile parser)]
    (core/parse compiled text)))

;;;-----------------------------------------------------------

(ns crustimoney.combinators-new
  (:refer-clojure :exclude [ref])
  (:require [crustimoney.combinators :as c]
            [crustimoney.results :as r]))

(defn ref [{:keys [to] :as args}]
  (let [scope  (-> args meta :scope)
        parser (delay (get @scope to))]
    (swap! scope assoc to nil)
    (fn
      ([_ index]
       (r/->push @parser index))
      ([_ _ result _]
       result))))

(defn literal [{s :text}]
  (c/literal s))

(defn chain [_ & children]
  (apply c/chain children))

(defn choice [_ & children]
  (apply c/choice children))

(defn repeat* [_ parser]
  (c/repeat* parser))

(defn negate [_ parser]
  (c/negate parser))

;;;-----------------------------------------------------------

(ns crustimoney.combinator-grammar
  (:require [crustimoney.combinators :as c]
            [crustimoney.core :as core]))

(defn- keyword-as-ref [parser]
  (if (keyword? parser) [:ref {:to parser}] parser))

(defn- keywords-as-refs [parsers]
  (map keyword-as-ref parsers))

(defn literal [text]
  [:literal {:text text}])

(defn chain [& parsers]
  (into [:chain] (keywords-as-refs parsers)))

(defn choice [& parsers]
  (into [:choice] (keywords-as-refs parsers)))

(defn repeat* [parser]
  [:repeat* (keyword-as-ref parser)])

(defn negate [parser]
  [:negate (keyword-as-ref parser)])
