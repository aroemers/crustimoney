(ns crustimoney2.combinators
  "Parsers combinator functions.

  Each combinator functions creates a parser function that is suitable
  for use with core's main parse function, and many take other parser
  functions as their argument; they are composable.

  If you want to implement your own parser combinator, read on.
  Otherwise, just look at the docstrings of the combinators
  themselves.

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
  `crustimoney2.results` namespace should be used for creating and
  reading these results.

  Before you write your own combinator, do realise that the provided
  combinators are complete in the sense that they can parse any text."
  (:require [crustimoney2.results :as r]))

;;; Primitives

(defn literal
  "A parser that matches an exact literal string."
  [s]
  (fn [text index]
    (let [end (+ index (count s))]
      (if (and (<= end (count text)) (= (subs text index end) s))
        (r/->success index end)
        #{(r/->error :expected-literal index {:literal s})}))))

(defn chain
  "Chain multiple consecutive parsers."
  [& parsers]
  (fn
    ([_text index]
     (if-let [parser (first parsers)]
       (r/->push parser index {:pindex 0 :children []})
       (r/->success index index)))

    ([_text _index result state]
     (if (r/success? result)
       (let [state (-> state (update :pindex inc) (update :children conj result))]
         (if-let [parser (nth parsers (:pindex state) nil)]
           (r/->push parser (r/success->end result) state)
           (r/->success (-> state :children first r/success->start)
                        (-> state :children last r/success->end)
                        (:children state))))
       result))))

(defn choice
  "Match the first of the ordered parsers that is successful."
  [& parsers]
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
  [parser]
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
  [parser]
  (fn
    ([_text index]
     (r/->push parser index))

    ([text index result _state]
     (if (r/success? result)
       #{(r/->error :unexpected-match index {:text (r/success->text result text)})}
       (r/->success index index)))))

;;; Extra combinators

(defn regex
  "A parser that matches the given regular expression."
  [re]
  (let [pattern (re-pattern (str "^(" re ")"))]
    (fn [text index]
      (if-let [[match] (re-find pattern (subs text index))]
        (r/->success index (+ index (count match)))
        #{(r/->error :expected-match index {:regex re})}))))

(defn repeat+
  "Eagerly try to match the parser as many times as possible, expecting
  at least one match."
  [parser]
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
  [parser]
  (fn
    ([_text index]
     (r/->push parser index))

    ([_text index result _state]
     (if (r/success? result)
       (r/->success index index)
       #{(r/->error :failed-lookahead index)}))))

(defn maybe
  "Try to parse the given parser, but succeed anyway."
  [parser]
  (fn
    ([_text index]
     (r/->push parser index))

    ([_text index result _state]
     (or (r/success? result)
         (r/->success index index)))))

(defn eof
  "Succeed only if the entire text has been parsed. Optionally another
  parser can be wrapped, after which the check is done when that parser
  is done (successfully). This means that `(chain a-parser eof)` behaves
  the same as `(eof a-parser)`, though the latter form evaluates to the
  result of the wrapped parser, whereas the former eof creates its own
  (empty) success."
  ([text index]
   (if (= (count text) index)
     (r/->success index index)
     #{(r/->error :eof-not-reached index)}))

  ([parser]
   (fn
     ([_text index]
      (r/->push parser index))

     ([text _index result _state]
      (if (r/success? result)
        (if (= (r/success->end result) (count text))
          result
          #{(r/->error :eof-not-reached (r/success->end result))})
        result)))))

;;; Cut support

(defn cut
  "Wrap the given parser with a cut. Backtracking will not occur past
  this point.

  Well placed cuts have two major benefits:

  - Substantial memory optimization, since the packrat caches can
  evict everything before the cut

  - Better error messages, since cuts prevent backtracking to the
  beginning of the text."
  [parser]
  (fn
    ([_text index]
     (r/->push parser index))

    ([_text _index result _state]
     (r/->cut result))))

;;; Result wrappers

(defn with-name
  "Wrap the parser, assigning a name to the (success) result of the
  parser. Nameless parsers are filtered out by default during
  parsing."
  [key parser]
  (fn [& args]
    (let [result (apply parser args)]
      (cond->> result (r/success? result) (r/with-success-name key)))))

(defn with-error
  "Wrap the parser, replacing any errors with a single error with the
  supplied error key."
  [key parser]
  (fn [text index & args]
    (let [result (apply parser text index args)]
      (if (set? result)
        #{(r/->error key index)}
        result))))

(defn with-value
  "Wrap the parser, adding a `:value` attribute to its success,
  containing the matched text. Optionally takes a function f, applied
  to the text value."
  ([parser]
   (with-value identity parser))
  ([f parser]
   (fn [text & args]
     (let [result (apply parser text args)]
       (cond-> result
         (r/success? result)
         (r/with-success-attrs {:value (f (r/success->text result text))}))))))
