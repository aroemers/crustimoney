(ns crustimoney.core
  "The main parsing functions."
  (:refer-clojure :exclude [compile])
  (:require [clojure.string :as str]
            [crustimoney.caches :as caches]
            [crustimoney.experimental.reader :as reader]
            [crustimoney.results :as r]
            [crustimoney.vector-grammar :as vector-grammar]))

(defn compile
  "Compile a parser model to its combinator function. This way the
  `parse` function can skip this step (speeding it up) and can the
  model be checked at compile time.

  For more information on the model, see the `vector-grammar`
  namespace."
  [model]
  (vector-grammar/compile model))


;;; Parse internals

(defn- named-self-or-children
  "Returns a sequence with `child` if it has a name, otherwise a
  sequence of its children that have a name."
  [child]
  (if (r/success->name child)
    [child]
    (filter r/success->name (r/success->children child))))

(defn- keep-named-children
  "Process a success result by keeping only the named children, merged
  with the named children of nameless children."
  [success]
  (r/with-success-children success
    (mapcat named-self-or-children (r/success->children success))))

(defn- infinite-loop?
  "Check if the stack already contains the parser at the index, if so,
  we have an infinite loop."
  [stack index parser]
  (loop [i (dec (count stack))]
    (when (<= 0 i)
      (let [stack-item (nth stack i)]
        (when (= (r/push->index stack-item) index)
          (if (= (r/push->parser stack-item) parser)
            true
            (recur (dec i))))))))

;;; Parsing virtual machine

(defn parse
  "Use the given `parser` to parse the supplied `text` string. The
  result will either be a success (a hiccup-style vector) or a set of
  errors. By default only named nodes are kept in a success
  result (the root node is allowed to be nameless).

  A success result looks like this:

      [:name {:start 0, :end 3}
       [:child-1 {:start 0, :end 2}]
       [:child-2 {:start 2, :end 3}]]

  An error result looks like this:

      ({:key :expected-literal, :at 0, :detail {:literal \"foo\"}}
       {:key :unexpected-match, :at 8, :detail {:text \"eve\"}})

  The parse function can take an `opts` map, with the following
  options:

  - `:index`, the index at which to start parsing in the text, default 0.

  - `:cache`, the packrat cache to use, see the caches namespace.
  Default is treemap-cache. To disable caching, use nil.

  - `:infinite-check?`, check for infinite loops during parsing.
  Default is true. Setting it to false yields a small performance
  boost."
  ([parser text]
   (parse parser text nil))
  ([parser text opts]
   ;; Options parsing
   (let [compiled        (compile parser)
         start-index     (:index opts 0)
         cache           (or (:cache opts (caches/treemap-cache)) caches/noop-cache)
         infinite-check? (:infinite-check? opts true)]

     ;; Main parsing loop
     (loop [stack  [(r/->push compiled start-index)]
            result nil
            state  nil
            cut-at 0]
       ;; See if there is something on the stack
       (if-let [stack-item (peek stack)]
         (let [parser (r/push->parser stack-item)
               index  (r/push->index stack-item)
               state' (r/push->state stack-item)

               ;; Call the parser
               result (if result
                        (parser text index result state)
                        (parser text index))]

           ;; Handle the parse result
           (cond
             ;; Handle a push result
             (r/push? result)
             (let [push-parser (r/push->parser result)
                   push-index  (r/push->index result)
                   push-state  (r/push->state result)]
               ;; Check for infinite loops
               (when (and infinite-check? (infinite-loop? stack push-index push-parser))
                 (throw (ex-info "Infinite parsing loop detected" {:index push-index})))
               ;; Check if this parser on this index is already in the cache
               (if-let [hit (caches/fetch cache push-parser push-index)]
                 (recur stack hit push-state cut-at)
                 (recur (conj stack result) nil nil cut-at)))

             ;; Handle a success result
             (r/success? result)
             (let [processed (keep-named-children result)]
               ;; Check if it was a hard-cut success
               (if (-> result meta :hard-cut)
                 (do (caches/cut cache (r/success->end result))
                     (when (satisfies? reader/CutSupport text)
                       (reader/cut text (r/success->end result)))
                     (recur (pop stack) processed state' (r/success->end result)))
                 (do (caches/store cache parser index processed)
                     (recur (pop stack) processed state' cut-at))))

             ;; Handle a set of errors
             (set? result)
             (if-not (or (-> result meta :soft-cut) (< index cut-at))
               (recur (pop stack) result state' cut-at)
               result)

             ;; Something weird was returned from the parser
             :else
             (let [info {:parser parser, :type (type result)}]
               (throw (ex-info "Unexpected result from parser" info)))))

         ;; Nothing on the stack, return result
         result)))))
