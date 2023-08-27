(ns crustimoney.combinators
  "Parsers combinator implementation functions.

  Although these functions can be used directly, the namespace
  `crustimoney.combinator-grammar` offers a nicer API. The
  documentation for the each of the combinators can be found there as
  well.

  If you want to implement your own parser combinator, read on.

  Each combinator here receives at least one argument, a property map.
  The rest of the arguments are the child parsers.

  The parsers returned by the combinators do not call other parsers
  directly, as this could lead to stack overflows. So next to a
  `->success` or `->error` result, it can also return a `->push`
  result. This pushes another parser onto the virtual stack.

  For this reason, a parser function has the following signature:

      (fn
        ([text index]
          ...)
        ([text index result state]
         ...))

  The 2-arity variant is called when the parser was pushed onto the
  stack. It receives the entire text and the index it should begin
  parsing. If it returns a `push` result, the 4-arity variant is
  called when that parser is done. It again receives the text and the
  original index, but also the result of the pushed parser and any
  state that was pushed with it.

  Both arities can return a success, a set of errors, or a push. The
  `crustimoney.results` namespace should be used for creating and
  reading these results.

  Before you write your own combinator, do realise that the provided
  combinators are complete in the sense that they can parse any text."
  (:refer-clojure :exclude [ref])
  (:require [crustimoney.experimental.reader :as reader]
            [crustimoney.results :as r]))

;;; Primitives

(defn literal
  "A parser that matches an exact literal string."
  [{:keys [text]}]
  (assert (string? text) "Literal must be a String")
  (let [s text]
    (fn [text index]
      (or (reader/match-literal text index s)
          #{(r/->error :expected-literal index {:literal s})}))))

(defn chain
  "Chain multiple consecutive parsers."
  [_ & parsers]
  (assert (not (#{:soft-cut :hard-cut} (first parsers)))
    "Cannot place a cut in first posision of a chain")
  (assert (empty? (remove #{:soft-cut :hard-cut} (filter keyword? parsers)))
    "Only :soft-cut and :hard-cut keywords are supported")

  (fn
    ([_text index]
     (if-let [parser (first parsers)]
       (r/->push parser index {:pindex 0 :children []})
       (r/->success index index)))

    ([_text _index result state]
     (if (r/success? result)
       (loop [state (-> state (update :pindex inc) (update :children conj result))]
         (if-let [parser (nth parsers (:pindex state) nil)]
           (condp = parser
             :soft-cut (recur (-> state (update :pindex inc) (assoc :soft-cut true)))
             :hard-cut (recur (-> state (update :pindex inc) (assoc :soft-cut true, :hard-cut true)))
             (r/->push parser (r/success->end result) state))
           (cond-> (r/->success (-> state :children first r/success->start)
                                (-> state :children last r/success->end)
                                (:children state))
             (:hard-cut state) (with-meta {:hard-cut true}))))

       (if (:soft-cut state)
         (with-meta result {:soft-cut true})
         result)))))

(defn choice
  "Match the first of the ordered parsers that is successful."
  [_ & parsers]
  (fn
    ([_text index]
     (if-let [parser (first parsers)]
       (r/->push parser index {:pindex 0 :children [] :errors #{}})
       (r/->success index index)))

    ([_text index result state]
     (if (r/success? result)
       (r/->success (r/success->start result) (r/success->end result) [result])
       (let [state (-> state (update :pindex inc) (update :errors into result))]
         (if-let [parser (nth parsers (:pindex state) nil)]
           (r/->push parser index state)
           (:errors state)))))))

(defn repeat*
  "Eagerly try to match the given parser as many times as possible."
  [_ parser]
  (fn
    ([_text index]
     (r/->push parser index {:children []}))

    ([_text index result state]
     (if (r/success? result)
       (let [state (update state :children conj result)]
         (r/->push parser (r/success->end result) state))
       (let [end (or (some-> state :children last r/success->end) index)]
         (r/->success index end (:children state)))))))

(defn negate
  "Negative lookahead for the given parser, i.e. this succeeds if the
  parser does not."
  [_ parser]
  (fn
    ([_text index]
     (r/->push parser index))

    ([text index result _state]
     (if (r/success? result)
       #{(r/->error :unexpected-match index {:text (r/success->text result text)})}
       (r/->success index index)))))

;;; Extra combinators

(defn regex
  "A parser that matches the given regular expression (string or
  pattern)."
  [{:keys [pattern]}]
  (let [pattern (re-pattern pattern)]
    (fn [text index]
      (or (reader/match-pattern text index pattern)
          #{(r/->error :expected-match index {:regex pattern})}))))

(defn repeat+
  "Eagerly try to match the parser as many times as possible, expecting
  at least one match."
  [_ parser]
  (fn
    ([_text index]
     (r/->push parser index {:children []}))

    ([_text index result state]
     (if (r/success? result)
       (r/->push parser (r/success->end result) (update state :children conj result))
       (if-let [children (seq (:children state))]
         (r/->success index (-> children last r/success->end) children)
         result)))))

(defn lookahead
  "Lookahead for the given parser, i.e. succeed if the parser does,
  without advancing the parsing position."
  [_ parser]
  (fn
    ([_text index]
     (r/->push parser index))

    ([_text index result _state]
     (if (r/success? result)
       (r/->success index index)
       result))))

(defn maybe
  "Try to parse the given parser, but succeed anyway."
  [_ parser]
  (fn
    ([_text index]
     (r/->push parser index))

    ([_text index result _state]
     (or (r/success? result)
         (r/->success index index)))))

(defn eof
  "Succeed only if the entire text has been parsed."
  [_]
  (negate (regex ".|\\n")))

;;; Result wrappers

(defn with-name
  "Wrap the parser, assigning a name to the (success) result of the
  parser. Nameless parsers are filtered out by default during
  parsing."
  [{:keys [key]} parser]
  (fn [& args]
    (let [result (apply parser args)]
      (cond->> result (r/success? result) (r/with-success-name key)))))

(defn with-error
  "Wrap the parser, replacing any errors with a single error with the
  supplied error key."
  [{:keys [key]} parser]
  (fn [text index & args]
    (let [result (apply parser text index args)]
      (if (set? result)
        #{(r/->error key index)}
        result))))

;;; Recursive grammar definition

(defn ref
  "Wrap another parser function, which is referred to by the given key.
  Only valid inside recursive grammars, for example:

      {:foo  (literal {:text \"foo\"})
       :root (ref {:to :foo})}"
  [{:keys [to] :as args}]
  (let [scope  (or (-> args meta :scope)
                   (throw (ex-info "Cannot use ref outside a recursive grammar" args)))
        parser (delay (get @scope to))]
    (swap! scope assoc to nil)
    (fn
      ([_ index]
       (r/->push @parser index))
      ([_ _ result _]
       result))))


;;; Explicit failure in model

(defn- ^:no-doc fail-to-compile
  "Internal combinator which fails to compile."
  [{:keys [error info]}]
  (throw (ex-info error info)))
