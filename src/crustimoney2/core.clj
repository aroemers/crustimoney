(ns crustimoney2.core
  "The main parsing functions."
  (:require [crustimoney2.results :as r]))

;;; Post success processing

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
  result (the root node is allowed to be nameless). The parse function
  takes an optional options map, with the following options:

  :index (default: 0)
  The index at which to start parsing in the text.

  :cache (default: no caching)
  The packrat caching function to use, see the caching namespaces.

  :keep-nameless (default: false)
  Set this to true if nameless success nodes should be kept in the
  parse result."
  ([parser text]
   (parse parser text nil))
  ([parser text opts]
   (let [start-index  (:index opts 0)
         cache        (:cache opts (constantly nil))
         post-success (if (:keep-nameless opts) identity keep-named-children)]
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
                   (if-let [hit (cache text push-parser push-index)]
                     (recur stack hit push-state)
                     (recur (conj stack result) nil nil)))

                 (r/success? result)
                 (let [processed (post-success result)]
                   (cache text parser index processed)
                   (recur (pop stack) processed state'))

                 (list? result)
                 (recur (pop stack) result state')

                 :else
                 (let [info {:parser parser, :type (type result)}]
                   (throw (ex-info "Unexpected result from parser" info)))))
         result)))))

;;; Recursive grammar definition

(defn- ref-fn [parsers]
  (fn [key]
    (fn [& args]
      (if-let [parser (get @parsers key)]
        (apply parser args)
        (throw (ex-info "Reference to unknown parser" {:key key}))))))

(defmacro rmap
  "Create a map of parsers that can recursively refer to other entries
  by using the (ref :some-key) function. For example:

  (rmap {:root (combinators/choice (ref :foo) (ref :bar))

         :foo (combinators/literal \"foo\")

         :bar (combinators/literal \"bar\")}

  The macro allows for multiple parser maps to be supplied. This way
  a map could refer to entries from another map. For example:

  (rmap basics {:entity-id (ref :uuid)})"
  [& parsers]
  `(let [parsers# (atom nil)
         ~'ref    (#'ref-fn parsers#)]
     (reset! parsers# (merge ~@parsers))))
