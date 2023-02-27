(ns crustimoney2.core
  "The main parsing functions."
  (:refer-clojure :exclude [ref])
  (:require [crustimoney2.caches :as c]
            [crustimoney2.results :as r]))

;;; Internals

(defn- keep-named-children
  "Process a success result by keeping only the named children, merged
  with the named children of nameless children. If that makes sense."
  [success]
  (let [children (mapcat (fn [child]
                           (if (r/success->name child)
                             [child]
                             (filter r/success->name (r/success->children child))))
                         (r/success->children success))]
    (r/with-success-children success children)))

;;; Parsing virtual machine

(defn parse
  "Use the given parser to parse the supplied text string. The result
  will either be a success (a hiccup-style vector) or a list of
  errors. By default only named nodes are kept in a success
  result (the root node is allowed to be nameless).

  A success result looks like this:

      [:name {:start 0, :end 3}
       [:child-1 {:start 0, :end 2, :value \"aa\"}]
       [:child-2 {:start 2, :end 3}]]

  An error result looks like this:

      ({:key :failed-lookahead, :at 0}
       {:key :expected-literal, :at 0, :detail {:literal \"foo\"}})

  The parse function can take an options map, with the following
  options:

  - `:index`, the index at which to start parsing in the text, default 0.

  - `:cache`, the packrat cache to use, see the caches namespace,
  default nil.

  - `:keep-nameless`, set this to true if nameless success nodes should
  be kept in the parse result, for debugging, defaults to false."
  ([parser text]
   (parse parser text nil))
  ([parser text opts]
   (let [start-index  (:index opts 0)
         cache        (or (:cache opts) c/noop-cache)
         post-success (if (:keep-nameless opts) identity keep-named-children)]
     ;; Main parsing loop
     (loop [stack  [(r/->push parser start-index)]
            result nil
            state  nil]
       (if-let [stack-item (peek stack)]
         (let [parser (r/push->parser stack-item)
               index  (r/push->index stack-item)
               state' (r/push->state stack-item)
               result (or (if result
                            (parser text index result state)
                            (parser text index))
                          ())]
           (cond (r/push? result)
                 (let [push-parser (r/push->parser result)
                       push-index  (r/push->index result)
                       push-state  (r/push->state result)]
                   (if-let [hit (c/fetch cache text push-parser push-index)]
                     (recur stack hit push-state)
                     (recur (conj stack result) nil nil)))

                 (r/success? result)
                 (let [processed (post-success result)]
                   (c/store cache text parser index processed)
                   (recur (pop stack) processed state'))

                 (list? result)
                 (recur (pop stack) result state')

                 :else
                 (let [info {:parser parser, :type (type result)}]
                   (throw (ex-info "Unexpected result from parser" info)))))
         result)))))

;;; Recursive grammar definition

(def ^:dynamic ^:no-doc *parsers*)

(defn ref
  "Creates a parser function that wraps another parser function, which
  is referred to by the given key. Needs to be called within the
  lexical scope of `rmap`."
  [key]
  (assert (bound? #'*parsers*)
    "Cannot use ref function outside rmap macro")
  (swap! *parsers* assoc key nil)
  (let [parsers *parsers*]
    (fn [& args]
      (apply (get @parsers key) args))))

(defn ^:no-doc rmap* [f]
  (binding [*parsers* (atom nil)]
    (let [result (swap! *parsers* merge (f))]
      (if-let [unknown-refs (seq (remove result (keys result)))]
        (throw (ex-info "Detected unknown keys in refs" {:unknown-keys unknown-refs}))
        result))))

(defmacro rmap
  "Takes (something that evaluates to) a map, in which the entries can
  refer to each other using the `ref` function. In other words, a
  recursive map. For example:

      (rmap {:foo  (literal \"foo\")
             :root (chain (ref :foo) \"bar\")})"
  [grammar]
  `(rmap* (fn [] ~grammar)))
