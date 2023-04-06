(ns crustimoney.combinators
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
  `crustimoney.results` namespace should be used for creating and
  reading these results.

  Before you write your own combinator, do realise that the provided
  combinators are complete in the sense that they can parse any text."
  (:refer-clojure :exclude [ref])
  (:require [crustimoney.results :as r]))

;;; Primitives

(defn literal
  "A parser that matches an exact literal string."
  [^String s]
  (let [size (count s)]
    (fn [^String text index]
      (if (.startsWith text s index)
        (r/->success index (+ index size))
        #{(r/->error :expected-literal index {:literal s})}))))

(defn chain
  "Chain multiple consecutive parsers.

  The chain combinator supports cuts. At least one normal parser must
  precede a cut. That parser must consume input, which no other
  parser (via a choice) up in the combinator tree could also consume
  at that point.

  Two kinds of cuts are supported. A \"hard\" cut and a \"soft\" cut,
  which can be inserted in the chain using `:hard-cut` or `:soft-cut`.
  Both types of cuts improve error messages, as they limit
  backtracking.

  With a hard cut, the parser is instructed to never backtrack before
  the end of this chain. A well placed hard cut has a major benefit,
  next to better error messages. It allows for substantial memory
  optimization, since the packrat caches can evict everything before
  the cut. This can turn memory requirements from O(n) to O(1). Since
  PEG parsers are memory hungry, this can be a big deal.

  With a soft cut, backtracking can still happen outside the chain,
  but errors will not escape inside the chain after a soft cut. The
  advantage of a soft cut over a hard cut, is that they can be used at
  more places without breaking the grammar.

  For example, the following parser benefits from a soft-cut:

      (choice (chain (maybe (chain (literal \"{\")
                                   :soft-cut
                                   (literal \"foo\")
                                   (literal \"}\")))
                     (literal \"bar\"))
              (literal \"baz\")))

  When parsing \"{foo\", it will nicely report that a \"}\" is
  missing. Without the soft-cut, it would report that \"bar\" or
  \"baz\" are expected, ignoring the more likely error.

  When parsing \"{foo}eve\", it will nicely report that \"bar\" or
  \"baz\" is missing. Placing a hard cut would only report \"bar\"
  missing, as it would never backtrack to try the \"baz\" choice.

  Soft cuts do not influence the packrat caches, so they do not help
  performance wise. A hard cut is implicitly also a soft cut."
  [& parsers]
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
       #{(r/->error :unexpected-match index {:text (r/success->text text result)})}
       (r/->success index index)))))

;;; Extra combinators

(defn regex
  "A parser that matches the given regular expression (string or
  pattern)."
  [re]
  (let [pattern (re-pattern re)]
    (fn [text index]
      (let [matcher (re-matcher pattern text)]
        (.region matcher index (count text))
        (if (.lookingAt matcher)
          (r/->success index (long (.end matcher)))
          #{(r/->error :expected-match index {:regex re})})))))

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
       result))))

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
  "Succeed only if the entire text has been parsed."
  []
  (negate (regex ".|\\n")))

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

;;; Recursive grammar definition

(def ^:dynamic ^:no-doc *parsers*)

(defn ref
  "Wrap another parser function, which is referred to by the given key.
  Needs to be called within the lexical scope of `grammar`."
  [key]
  (assert (bound? #'*parsers*)
    "Cannot use ref function outside grammar macro")

  (swap! *parsers* assoc key nil)
  (let [parsers *parsers*
        parser  (delay (get @parsers key))]
    (fn
      ([_ index]
       (r/->push @parser index))
      ([_ _ result _]
       result))))

(defn- auto-capture [m]
  (reduce-kv (fn [a k v]
               (let [rule-name     (name k)
                     auto-capture? (= (last rule-name) \=)
                     rule-key      (keyword (cond-> rule-name auto-capture? (subs 0 (dec (count rule-name)))))
                     rule-expr     (cond->> v auto-capture? (with-name rule-key))]
                 (assoc a rule-key rule-expr)))
             {} m))

(defn ^:no-doc grammar* [f]
  (if (bound? #'*parsers*)
    (f)
    (binding [*parsers* (atom nil)]
      (let [result (swap! *parsers* merge (auto-capture (f)))]
        (if-let [unknown-refs (seq (remove result (keys result)))]
          (throw (ex-info "Detected unknown keys in refs" {:unknown-keys unknown-refs}))
          result)))))

(defmacro grammar
  "Takes one or more maps, in which the entries can refer to each other
  using the `ref` function. In other words, a recursive map. For
  example:

      (grammar {:foo  (literal \"foo\")
                :root (chain (ref :foo) \"bar\")})

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
  [& maps]
  `(grammar* (fn [] (merge ~@maps))))
