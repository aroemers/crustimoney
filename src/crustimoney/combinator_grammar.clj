(ns crustimoney.combinator-grammar
  "This grammar allows you to create a parser (vector) model using
  functions.

  Each combinator functions creates a parser model that is suitable
  for use with core's main parse function, and many take other parser
  functions as their argument; they are composable.

  The functions in this namespace are in direct relation to the actual
  `crustimoney.combinators` they compile to, except for `ref`. To
  refer to other parsers in a recursive grammar, you simply use its
  keyword. For example:

      {:root (chain (literal \"foo\") :bax)
       :bax  (choice (literal \"bar\")
                     (literal \"baz\"))}

  As with any recursive grammar, you can auto-capture a rule's parser
  by adding the `=` postfix to its name."
  (:refer-clojure :exclude [>]))

;;; Keywords as refs

(defn- keyword-as-ref [parser]
  (if (keyword? parser) [:ref {:to parser}] parser))

(defn- keywords-as-refs [parsers]
  (map keyword-as-ref parsers))


;;; Primitives

(defn literal
  "A parser that matches an exact literal string."
  [text]
  [:literal {:text text}])

(def soft-cut "A soft-cut for use within `chain`."
  [:fail-to-compile {:error "Cannot use cut outside chain", :info {:type :soft}}])

(def hard-cut "A hard-cut for use within `chain`."
  [:fail-to-compile {:error "Cannot use cut outside chain", :info {:type :hard}}])

(def > "An alias for soft-cut." soft-cut)

(def >> "An alias for hard-cut." hard-cut)

(defn chain
  "Chain multiple consecutive parsers.

  The chain combinator supports cuts. At least one normal parser must
  precede a cut. That parser must consume input, which no other
  parser (via a choice) up in the combinator tree could also consume
  at that point.

  Two kinds of cuts are supported. A \"hard\" cut and a \"soft\" cut,
  which can be inserted in the chain using `hard-cut` (or `>>`) and
  `soft-cut` (or `>`). Both types of cuts improve error messages, as
  they limit backtracking.

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
                                   soft-cut
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
  (into [:chain] (replace {soft-cut :soft-cut, hard-cut :hard-cut}
                          (keywords-as-refs parsers))))

(defn choice
  "Match the first of the ordered parsers that is successful."
  [& parsers]
  (into [:choice] (keywords-as-refs parsers)))

(defn repeat*
  "Eagerly try to match the given parser as many times as possible."
  [parser]
  [:repeat* (keyword-as-ref parser)])

(defn negate
  "Negative lookahead for the given parser, i.e. this succeeds if the
  parser does not."
  [parser]
  [:negate (keyword-as-ref parser)])

;;; Extra combinators

(defn regex
  "A parser that matches the given regular expression (string or
  pattern)."
  [pattern]
  [:regex {:pattern pattern}])

(defn repeat+
  "Eagerly try to match the parser as many times as possible, expecting
  at least one match."
  [parser]
  [:repeat+ (keyword-as-ref parser)])

(defn lookahead
  "Lookahead for the given parser, i.e. succeed if the parser does,
  without advancing the parsing position."
  [parser]
  [:lookahead (keyword-as-ref parser)])

(defn maybe
  "Try to parse the given parser, but succeed anyway."
  [parser]
  [:maybe (keyword-as-ref parser)])

(defn eof
  "Succeed only if the entire text has been parsed."
  []
  [:eof])

;;; Result wrappers

(defn with-name
  "Wrap the parser, assigning a name to the (success) result of the
  parser. Nameless parse results are filtered out during parsing."
  [key parser]
  [:with-name {:key key} (keyword-as-ref parser)])

(defn with-error
  "Wrap the parser, replacing any errors with a single error with the
  supplied error key."
  [key parser]
  [:with-error {:key key} (keyword-as-ref parser)])
