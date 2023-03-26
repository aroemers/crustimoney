# Table of contents
-  [`crustimoney.caches`](#crustimoney.caches)  - Packrat caches for the core/parse function.
    -  [`Cache`](#crustimoney.caches/Cache) - Protocol for packrat cache implementations.
    -  [`cut`](#crustimoney.caches/cut) - Clear all cached results before given index.
    -  [`fetch`](#crustimoney.caches/fetch) - Try to fetch a cached result, returns nil if it misses the cache.
    -  [`store`](#crustimoney.caches/store) - Store a result in the cache.
    -  [`treemap-cache`](#crustimoney.caches/treemap-cache) - Create a cache that supports clearing below a certain index, such that entries are evicted on cuts.
    -  [`weak-treemap-cache`](#crustimoney.caches/weak-treemap-cache) - Create a cache that supports clearing below a certain index and has weak references, such that entries are evicted on cuts or on memory pressure.
-  [`crustimoney.combinators`](#crustimoney.combinators)  - Parsers combinator functions.
    -  [`chain`](#crustimoney.combinators/chain) - Chain multiple consecutive parsers.
    -  [`choice`](#crustimoney.combinators/choice) - Match the first of the ordered parsers that is successful.
    -  [`eof`](#crustimoney.combinators/eof) - Succeed only if the entire text has been parsed.
    -  [`grammar`](#crustimoney.combinators/grammar) - Takes (something that evaluates to) a map, in which the entries can refer to each other using the <code>ref</code> function.
    -  [`literal`](#crustimoney.combinators/literal) - A parser that matches an exact literal string.
    -  [`lookahead`](#crustimoney.combinators/lookahead) - Lookahead for the given parser, i.e.
    -  [`maybe`](#crustimoney.combinators/maybe) - Try to parse the given parser, but succeed anyway.
    -  [`negate`](#crustimoney.combinators/negate) - Negative lookahead for the given parser, i.e.
    -  [`ref`](#crustimoney.combinators/ref) - Wrap another parser function, which is referred to by the given key.
    -  [`regex`](#crustimoney.combinators/regex) - A parser that matches the given regular expression.
    -  [`repeat*`](#crustimoney.combinators/repeat*) - Eagerly try to match the given parser as many times as possible.
    -  [`repeat+`](#crustimoney.combinators/repeat+) - Eagerly try to match the parser as many times as possible, expecting at least one match.
    -  [`with-error`](#crustimoney.combinators/with-error) - Wrap the parser, replacing any errors with a single error with the supplied error key.
    -  [`with-name`](#crustimoney.combinators/with-name) - Wrap the parser, assigning a name to the (success) result of the parser.
-  [`crustimoney.combinators.experimental`](#crustimoney.combinators.experimental)  - Experimental combinators.
    -  [`range`](#crustimoney.combinators.experimental/range) - Like a repeat, but the times the wrapped <code>parser</code> is matched must lie within the given range.
    -  [`recover`](#crustimoney.combinators.experimental/recover) - Like <code>choice</code>, capturing errors of the first choice, including soft-cuts in its scope.
    -  [`stream`](#crustimoney.combinators.experimental/stream) - Like <code>repeat*</code>, but pushes results to the <code>callback</code> function, instead of returning them as children.
    -  [`success->recovered-errors`](#crustimoney.combinators.experimental/success->recovered-errors) - Returns the recovered errors from a result, as set by the <code>recover</code> combinator parser.
-  [`crustimoney.core`](#crustimoney.core)  - The main parsing functions.
    -  [`parse`](#crustimoney.core/parse) - Use the given parser to parse the supplied text string.
-  [`crustimoney.data-grammar`](#crustimoney.data-grammar)  - Create a parser based on a data grammar.
    -  [`DataGrammar`](#crustimoney.data-grammar/DataGrammar)
    -  [`create-parser`](#crustimoney.data-grammar/create-parser) - Create a parser based on a data grammar definition.
    -  [`vector-tree`](#crustimoney.data-grammar/vector-tree) - Low-level protocol function which translates the data type into an intermediary vector-based representation.
-  [`crustimoney.results`](#crustimoney.results)  - Result constructors, accessors and predicates.
    -  [`->error`](#crustimoney.results/->error) - Create an error result, given an error key and an index.
    -  [`->push`](#crustimoney.results/->push) - Create a push value, given a parser function and an index.
    -  [`->success`](#crustimoney.results/->success) - Create a success result, given a start index (inclusive) and end index (exclusive).
    -  [`error->detail`](#crustimoney.results/error->detail) - Return the detail object of an error.
    -  [`error->index`](#crustimoney.results/error->index) - Return the index of an error.
    -  [`error->key`](#crustimoney.results/error->key) - Return the key of an error.
    -  [`errors->line-column`](#crustimoney.results/errors->line-column) - Returns the errors with <code>:line</code> and <code>:column</code> entries added.
    -  [`push->index`](#crustimoney.results/push->index) - Returns the index of a push value.
    -  [`push->parser`](#crustimoney.results/push->parser) - Returns the parser of a push value.
    -  [`push->state`](#crustimoney.results/push->state) - Returns the state of a push value.
    -  [`push?`](#crustimoney.results/push?) - Returns obj if obj is a push value.
    -  [`success->children`](#crustimoney.results/success->children) - Returns the children of a success.
    -  [`success->end`](#crustimoney.results/success->end) - Return the end index of a success.
    -  [`success->name`](#crustimoney.results/success->name) - Return the name of a success.
    -  [`success->start`](#crustimoney.results/success->start) - Return the start index of a success.
    -  [`success->text`](#crustimoney.results/success->text) - Returns the matched text of a success, given the full text.
    -  [`success?`](#crustimoney.results/success?) - Returns obj if obj is a success value, nil otherwise.
-  [`crustimoney.string-grammar`](#crustimoney.string-grammar)  - Create a parser based on a string grammar.
    -  [`create-parser`](#crustimoney.string-grammar/create-parser) - Create a parser based on a string-based grammar definition.
    -  [`vector-tree`](#crustimoney.string-grammar/vector-tree) - Low-level function which translates the string grammar into an intermediary vector-based representation.
-  [`crustimoney.vector-grammar`](#crustimoney.vector-grammar)  - A basic vector-driven parser generator.
    -  [`create-parser`](#crustimoney.vector-grammar/create-parser) - Create a parser based on a vector-driven combinator tree.

-----
# <a name="crustimoney.caches">crustimoney.caches</a>


Packrat caches for the core/parse function.

  A single cache instance is *not* intended to be used for multiple
  texts. Create a new cache for each new text.

  Caches are implemented using the Cache protocol. This way you can
  implement your own, if desired.




## <a name="crustimoney.caches/Cache">`Cache`</a><a name="crustimoney.caches/Cache"></a>




Protocol for packrat cache implementations.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/caches.clj#L10-L20">Source</a></sub></p>

## <a name="crustimoney.caches/cut">`cut`</a><a name="crustimoney.caches/cut"></a>
``` clojure

(cut this index)
```

Clear all cached results before given index.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/caches.clj#L19-L20">Source</a></sub></p>

## <a name="crustimoney.caches/fetch">`fetch`</a><a name="crustimoney.caches/fetch"></a>
``` clojure

(fetch this parser index)
```

Try to fetch a cached result, returns nil if it misses the cache.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/caches.clj#L13-L14">Source</a></sub></p>

## <a name="crustimoney.caches/store">`store`</a><a name="crustimoney.caches/store"></a>
``` clojure

(store this parser index result)
```

Store a result in the cache.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/caches.clj#L16-L17">Source</a></sub></p>

## <a name="crustimoney.caches/treemap-cache">`treemap-cache`</a><a name="crustimoney.caches/treemap-cache"></a>
``` clojure

(treemap-cache)
```

Create a cache that supports clearing below a certain index, such
  that entries are evicted on cuts.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/caches.clj#L30-L47">Source</a></sub></p>

## <a name="crustimoney.caches/weak-treemap-cache">`weak-treemap-cache`</a><a name="crustimoney.caches/weak-treemap-cache"></a>
``` clojure

(weak-treemap-cache)
```

Create a cache that supports clearing below a certain index and has
  weak references, such that entries are evicted on cuts or on memory
  pressure.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/caches.clj#L49-L67">Source</a></sub></p>

-----
# <a name="crustimoney.combinators">crustimoney.combinators</a>


Parsers combinator functions.

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
  [`crustimoney.results`](#crustimoney.results) namespace should be used for creating and
  reading these results.

  Before you write your own combinator, do realise that the provided
  combinators are complete in the sense that they can parse any text.




## <a name="crustimoney.combinators/chain">`chain`</a><a name="crustimoney.combinators/chain"></a>
``` clojure

(chain & parsers)
```

Chain multiple consecutive parsers.

  The chain combinator supports cuts. At least one normal parser must
  precede a cut. That parser must consume input, which no other
  parser (via a choice) up in the combinator tree could also consume
  at that point.

  Two kinds of cuts are supported. A "hard" cut and a "soft" cut,
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

  For example, the following grammar benefits from a soft-cut:

      {:prefix (chain (literal "[")
                      :soft-cut
                      (maybe (ref :expr))
                      (literal "]"))

       :expr   (choice (with-name :foo
                         (chain (maybe (ref :prefix))
                                (literal "foo")))
                       (with-name :bar
                         (chain (maybe (ref :prefix))
                                (literal "bar"))))}

  When parsing "[foo", it will nicely report that a "]" is
  missing. Without the soft-cut, it would report that "foo" or
  "bar" are expected, ignoring that clearly a prefix was started.

  When parsing "[foo]bar", this succeeds nicely. Placing a hard cut
  at the location of the soft-cut would fail to parse this, as it
  would never backtrack to try the prefix with "bar" after it.

  Soft cuts do not influence the packrat caches, so they do not help
  performance wise. A hard cut is implicitly also a soft cut.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/combinators.clj#L52-L128">Source</a></sub></p>

## <a name="crustimoney.combinators/choice">`choice`</a><a name="crustimoney.combinators/choice"></a>
``` clojure

(choice & parsers)
```

Match the first of the ordered parsers that is successful.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/combinators.clj#L130-L145">Source</a></sub></p>

## <a name="crustimoney.combinators/eof">`eof`</a><a name="crustimoney.combinators/eof"></a>
``` clojure

(eof)
```

Succeed only if the entire text has been parsed.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/combinators.clj#L226-L229">Source</a></sub></p>

## <a name="crustimoney.combinators/grammar">`grammar`</a><a name="crustimoney.combinators/grammar"></a>
``` clojure

(grammar m)
```
Function.

Takes (something that evaluates to) a map, in which the entries can
  refer to each other using the [[`ref`](#crustimoney.combinators/ref)](#crustimoney.combinators/ref) function. In other words, a
  recursive map. For example:

      (grammar {:foo  (literal "foo")
                :root (chain (ref :foo) "bar")})

  A rule's name key can be postfixed with `=`. The rule's parser is
  then wrapped with [`with-name`](#crustimoney.combinators/with-name) (without the postfix). A [[`ref`](#crustimoney.combinators/ref)](#crustimoney.combinators/ref) to such
  rule is also without the postfix.

  However, it is encouraged to be very intentional about which nodes
  should be captured and when. For example, the following (string)
  grammar ensures that the `:prefixed` node is only in the result when
  applicable.

      root=    <- prefixed (' ' prefixed)*
      prefixed <- (:prefixed '!' body) / body
      body=    <- [a-z]+

  Parsing "foo !bar" would result in the following result tree:

      [:root {:start 0, :end 8}
       [:body {:start 0, :end 3}]
       [:prefixed {:start 4, :end 8}
        [:body {:start 5, :end 8}]]]
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/combinators.clj#L289-L317">Source</a></sub></p>

## <a name="crustimoney.combinators/literal">`literal`</a><a name="crustimoney.combinators/literal"></a>
``` clojure

(literal s)
```

A parser that matches an exact literal string.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/combinators.clj#L43-L50">Source</a></sub></p>

## <a name="crustimoney.combinators/lookahead">`lookahead`</a><a name="crustimoney.combinators/lookahead"></a>
``` clojure

(lookahead parser)
```

Lookahead for the given parser, i.e. succeed if the parser does,
  without advancing the parsing position.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/combinators.clj#L202-L213">Source</a></sub></p>

## <a name="crustimoney.combinators/maybe">`maybe`</a><a name="crustimoney.combinators/maybe"></a>
``` clojure

(maybe parser)
```

Try to parse the given parser, but succeed anyway.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/combinators.clj#L215-L224">Source</a></sub></p>

## <a name="crustimoney.combinators/negate">`negate`</a><a name="crustimoney.combinators/negate"></a>
``` clojure

(negate parser)
```

Negative lookahead for the given parser, i.e. this succeeds if the
  parser does not.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/combinators.clj#L161-L172">Source</a></sub></p>

## <a name="crustimoney.combinators/ref">`ref`</a><a name="crustimoney.combinators/ref"></a>
``` clojure

(ref key)
```

Wrap another parser function, which is referred to by the given key.
  Needs to be called within the lexical scope of [`grammar`](#crustimoney.combinators/grammar).
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/combinators.clj#L256-L269">Source</a></sub></p>

## <a name="crustimoney.combinators/regex">`regex`</a><a name="crustimoney.combinators/regex"></a>
``` clojure

(regex re)
```

A parser that matches the given regular expression.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/combinators.clj#L176-L185">Source</a></sub></p>

## <a name="crustimoney.combinators/repeat*">`repeat*`</a><a name="crustimoney.combinators/repeat*"></a>
``` clojure

(repeat* parser)
```

Eagerly try to match the given parser as many times as possible.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/combinators.clj#L147-L159">Source</a></sub></p>

## <a name="crustimoney.combinators/repeat+">`repeat+`</a><a name="crustimoney.combinators/repeat+"></a>
``` clojure

(repeat+ parser)
```

Eagerly try to match the parser as many times as possible, expecting
  at least one match.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/combinators.clj#L187-L200">Source</a></sub></p>

## <a name="crustimoney.combinators/with-error">`with-error`</a><a name="crustimoney.combinators/with-error"></a>
``` clojure

(with-error key parser)
```

Wrap the parser, replacing any errors with a single error with the
  supplied error key.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/combinators.clj#L242-L250">Source</a></sub></p>

## <a name="crustimoney.combinators/with-name">`with-name`</a><a name="crustimoney.combinators/with-name"></a>
``` clojure

(with-name key parser)
```

Wrap the parser, assigning a name to the (success) result of the
  parser. Nameless parsers are filtered out by default during
  parsing.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/combinators.clj#L233-L240">Source</a></sub></p>

-----
# <a name="crustimoney.combinators.experimental">crustimoney.combinators.experimental</a>


Experimental combinators. These may get promoted, or changed,
  or dismissed.

  These combinators are not available in the string- or data-driven
  grammar (yet). To use them with those, you can use the
  `other-parsers` parameter of their respective `create-parser`
  functions, like:

      (require '[crustimoney.combinators.experimental :as e])

      (create-parser
        "root= <- stream
         expr= <- '{' [0-9]+ '}'"
        {:stream [::e/stream handle-expr
                  [::e/recover [:ref :expr] [:regex ".*?}"]]})

  Note that the other-parsers here is written in vector-grammar
  format. This is a little power-user trick, and allows you to declare
  `ref`s that do not have to resolve immediatly on their creation.




## <a name="crustimoney.combinators.experimental/range">`range`</a><a name="crustimoney.combinators.experimental/range"></a>
``` clojure

(range parser min max)
```

Like a repeat, but the times the wrapped `parser` is matched must lie
  within the given range. It will not try to parse more than `max`
  times.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/combinators/experimental.clj#L106-L129">Source</a></sub></p>

## <a name="crustimoney.combinators.experimental/recover">`recover`</a><a name="crustimoney.combinators.experimental/recover"></a>
``` clojure

(recover parser & recoverers)
```

Like `choice`, capturing errors of the first choice, including
  soft-cuts in its scope.

  If the first `parser` fails, the `recoverers` parsers are tried in
  order. If one of those succeeds, it results in a success node like
  this:

      [:crusti/recovered {:start .., :end .., :errors #{..}}]

  The errors are those of the first parser, and can be extracted using
  [`success->recovered-errors`](#crustimoney.combinators.experimental/success->recovered-errors). If all recovery parsers fail, the
  result will also be the errors of first parser.

  As with any parser, the name can be changed using `with-name`.

  Example usage:

      (repeat* (recover (with-name :content
                          (chain (literal "{")
                                 (regex #"\d+")
                                 (literal "}")))
                        (regex ".*?}")))

  Parsing something like `{42}{nan}{100}` would result in:

      [nil {:start 0, :end 14}
       [:content {:start 0, :end 4}]
       [:crusti/recovered {:start 4, :end 9, :errors #{{:key :expected-match, :at 5, ...}}}]
       [:content {:start 9, :end 14}]]
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/combinators/experimental.clj#L53-L104">Source</a></sub></p>

## <a name="crustimoney.combinators.experimental/stream">`stream`</a><a name="crustimoney.combinators.experimental/stream"></a>
``` clojure

(stream callback parser)
```

Like `repeat*`, but pushes results to the `callback` function,
  instead of returning them as children.

  If `callback` is a symbol, it is resolved using `requiring-resolve`.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/combinators/experimental.clj#L24-L40">Source</a></sub></p>

## <a name="crustimoney.combinators.experimental/success->recovered-errors">`success->recovered-errors`</a><a name="crustimoney.combinators.experimental/success->recovered-errors"></a>
``` clojure

(success->recovered-errors success)
```

Returns the recovered errors from a result, as set by the
  [`recover`](#crustimoney.combinators.experimental/recover) combinator parser.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/combinators/experimental.clj#L42-L46">Source</a></sub></p>

-----
# <a name="crustimoney.core">crustimoney.core</a>


The main parsing functions.




## <a name="crustimoney.core/parse">`parse`</a><a name="crustimoney.core/parse"></a>
``` clojure

(parse parser text)
(parse parser text opts)
```

Use the given parser to parse the supplied text string. The result
  will either be a success (a hiccup-style vector) or a set of
  errors. By default only named nodes are kept in a success
  result (the root node is allowed to be nameless).

  A success result looks like this:

      [:name {:start 0, :end 3}
       [:child-1 {:start 0, :end 2}]
       [:child-2 {:start 2, :end 3}]]

  An error result looks like this:

      ({:key :expected-literal, :at 0, :detail {:literal "foo"}}
       {:key :unexpected-match, :at 8, :detail {:text "eve"}})

  The parse function can take an options map, with the following
  options:

  - `:index`, the index at which to start parsing in the text, default 0.

  - `:cache`, the packrat cache to use, see the caches namespace.
  Default is treemap-cache. To disable caching, use nil.

  - `:infinite-check?`, check for infinite loops during parsing.
  Default is true. Setting it to false yields a small performance
  boost.

  - `:keep-nameless?`, set this to true if nameless success nodes
  should be kept in the parse result. This can be useful for
  debugging. Defaults to false.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/core.clj#L37-L140">Source</a></sub></p>

-----
# <a name="crustimoney.data-grammar">crustimoney.data-grammar</a>


Create a parser based on a data grammar. The data is translated into
  combinators.




## <a name="crustimoney.data-grammar/DataGrammar">`DataGrammar`</a><a name="crustimoney.data-grammar/DataGrammar"></a>



<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/data_grammar.clj#L13-L28">Source</a></sub></p>

## <a name="crustimoney.data-grammar/create-parser">`create-parser`</a><a name="crustimoney.data-grammar/create-parser"></a>
``` clojure

(create-parser data)
(create-parser data other-parsers)
```

Create a parser based on a data grammar definition. If a map with
  rules is supplied, a map of parsers is returned. Otherwise a single
  parser is returned. The following example shows what a data grammar
  looks like:

      {;; terminals
       literal            "foo"
       character          \c
       regex              #"[a-z]"
       regex-tag          #crusti/regex "[a-z]" ; EDN support
       eof                $

       ;; refs, chains, choices and grouping
       reference          literal
       chain              (literal regex)
       choices            (literal / regex / "alice" "bob")
       named-group        (:my-name literal / "the end" $)
       auto-named-group=  (literal / "the end" $)

       ;; quantifiers
       zero-to-many       (literal *)
       one-to-many        ("bar"+)
       zero-to-one        ("foo" "bar"?) ; bar is optional here

       ;; lookaheads
       lookahead          (& regex)
       negative-lookahead (!"alice")

       ;; cuts
       soft-cut           ('[' > expr? ']') ; note the >
       hard-cut           ((class-open class class-close >>)*) ; note the >>

       ;; direct combinator calls
       combinator-call    [:with-error :fail #crusti/parser ("fooba" #"r|z")]
       custom-combinator  [:my.app/my-combinator ...]}

  To capture nodes in the parse result, you need to use named groups.
  If you postfix a rule name with `=`, the expression is automatically
  captured using the rule's name (without the postfix). Please read up
  on this at [`crustimoney.combinators/grammar`](#crustimoney.combinators/grammar).

  Optionally an existing map of parsers can be supplied, which can
  refered to by the data grammar. For example:

      (create-parser '{root ("Hello " email)}
                     {:email (regex "...")})

  If you want to use an EDN grammar file or string, you can use
  `#crusti/regex` tagged literal for regular expressions. To read
  this, use the following:

      (clojure.edn/read-string {:readers *data-readers*} ...)
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/data_grammar.clj#L104-L162">Source</a></sub></p>

## <a name="crustimoney.data-grammar/vector-tree">`vector-tree`</a><a name="crustimoney.data-grammar/vector-tree"></a>
``` clojure

(vector-tree data)
```

Low-level protocol function which translates the data type
  into an intermediary vector-based representation. See
  [`crustimoney.vector-grammar`](#crustimoney.vector-grammar) for more on this format. This can be
  useful for debugging, or adding your own data type.

  In the latter case, add your type like so:

      (extend-type java.util.Date
        DataGrammar
        (vector-tree [date]
          [:my-namespace/my-flexible-date-parser date]))

  To see which data types are already supported, use `(->
  DataGrammar :impls keys)`
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/data_grammar.clj#L14-L28">Source</a></sub></p>

-----
# <a name="crustimoney.results">crustimoney.results</a>


Result constructors, accessors and predicates




## <a name="crustimoney.results/->error">`->error`</a><a name="crustimoney.results/->error"></a>
``` clojure

(->error key index)
(->error key index detail)
```

Create an error result, given an error key and an index. An extra
  detail object can be added.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/results.clj#L61-L67">Source</a></sub></p>

## <a name="crustimoney.results/->push">`->push`</a><a name="crustimoney.results/->push"></a>
``` clojure

(->push parser index)
(->push parser index state)
```

Create a push value, given a parser function and an index. Optionally
  a state object can be added.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/results.clj#L86-L92">Source</a></sub></p>

## <a name="crustimoney.results/->success">`->success`</a><a name="crustimoney.results/->success"></a>
``` clojure

(->success start end)
(->success start end children)
```

Create a success result, given a start index (inclusive) and end
  index (exclusive). Optionally a collection of success children can
  be given. The name of the success is nil.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/results.clj#L6-L13">Source</a></sub></p>

## <a name="crustimoney.results/error->detail">`error->detail`</a><a name="crustimoney.results/error->detail"></a>
``` clojure

(error->detail error)
```

Return the detail object of an error.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/results.clj#L79-L82">Source</a></sub></p>

## <a name="crustimoney.results/error->index">`error->index`</a><a name="crustimoney.results/error->index"></a>
``` clojure

(error->index error)
```

Return the index of an error
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/results.clj#L74-L77">Source</a></sub></p>

## <a name="crustimoney.results/error->key">`error->key`</a><a name="crustimoney.results/error->key"></a>
``` clojure

(error->key error)
```

Return the key of an error.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/results.clj#L69-L72">Source</a></sub></p>

## <a name="crustimoney.results/errors->line-column">`errors->line-column`</a><a name="crustimoney.results/errors->line-column"></a>
``` clojure

(errors->line-column text errors)
```

Returns the errors with `:line` and `:column` entries added.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/results.clj#L133-L139">Source</a></sub></p>

## <a name="crustimoney.results/push->index">`push->index`</a><a name="crustimoney.results/push->index"></a>
``` clojure

(push->index push)
```

Returns the index of a push value.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/results.clj#L105-L108">Source</a></sub></p>

## <a name="crustimoney.results/push->parser">`push->parser`</a><a name="crustimoney.results/push->parser"></a>
``` clojure

(push->parser push)
```

Returns the parser of a push value.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/results.clj#L100-L103">Source</a></sub></p>

## <a name="crustimoney.results/push->state">`push->state`</a><a name="crustimoney.results/push->state"></a>
``` clojure

(push->state push)
```

Returns the state of a push value.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/results.clj#L110-L113">Source</a></sub></p>

## <a name="crustimoney.results/push?">`push?`</a><a name="crustimoney.results/push?"></a>
``` clojure

(push? obj)
```

Returns obj if obj is a push value.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/results.clj#L94-L98">Source</a></sub></p>

## <a name="crustimoney.results/success->children">`success->children`</a><a name="crustimoney.results/success->children"></a>
``` clojure

(success->children success)
```

Returns the children of a success.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/results.clj#L31-L34">Source</a></sub></p>

## <a name="crustimoney.results/success->end">`success->end`</a><a name="crustimoney.results/success->end"></a>
``` clojure

(success->end success)
```

Return the end index of a success.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/results.clj#L26-L29">Source</a></sub></p>

## <a name="crustimoney.results/success->name">`success->name`</a><a name="crustimoney.results/success->name"></a>
``` clojure

(success->name success)
```

Return the name of a success.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/results.clj#L36-L39">Source</a></sub></p>

## <a name="crustimoney.results/success->start">`success->start`</a><a name="crustimoney.results/success->start"></a>
``` clojure

(success->start success)
```

Return the start index of a success.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/results.clj#L21-L24">Source</a></sub></p>

## <a name="crustimoney.results/success->text">`success->text`</a><a name="crustimoney.results/success->text"></a>
``` clojure

(success->text text success)
```

Returns the matched text of a success, given the full text.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/results.clj#L41-L44">Source</a></sub></p>

## <a name="crustimoney.results/success?">`success?`</a><a name="crustimoney.results/success?"></a>
``` clojure

(success? obj)
```

Returns obj if obj is a success value, nil otherwise.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/results.clj#L15-L19">Source</a></sub></p>

-----
# <a name="crustimoney.string-grammar">crustimoney.string-grammar</a>


Create a parser based on a string grammar. The grammar is translated
  into combinators.




## <a name="crustimoney.string-grammar/create-parser">`create-parser`</a><a name="crustimoney.string-grammar/create-parser"></a>
``` clojure

(create-parser text)
(create-parser text other-parsers)
```

Create a parser based on a string-based grammar definition. If the
  definition contains multiple rules, a map of parsers is returned.
  The following definition describes the string grammar syntax in
  itself:

      space            <- [\s]*

      non-terminal=    <- [a-zA-Z_-]+
      literal          <- '\'' > (:literal #'(\\\'|[^\'])*') '\''
      character-class= <- '[' > #'(\\]|[^]])*' ']' [?*+]?
      regex=           <- '#' > literal
      ref              <- (non-terminal !'=' space !'<-')
      end-of-file=     <- '$'

      group-name       <- ':' > (:group-name [a-zA-Z_-]+)
      group=           <- '(' > group-name? space choice space ')'

      expr             <- ref / group / literal / character-class / end-of-file / regex

      quantified       <- (:quantified expr (:operand [?+*])) / expr
      lookahead        <- (:lookahead (:operand [&!]) > quantified) / quantified

      cut=             <- '>>' / '>'

      chain            <- (:chain lookahead (space (cut / lookahead))+) / lookahead
      choice           <- (:choice chain (space '/' space chain)+) / chain

      rule=            <- (:rule-name non-terminal '='?) space '<-' >> space choice
      root=            <- (:rules (space rule space)+) / (:no-rules space choice space) $

  To capture nodes in the parse result, you need to use named groups.
  If you postfix a rule name with `=`, the expression is automatically
  captured using the rule's name (without the postfix). Please read up
  on this at [`crustimoney.combinators/grammar`](#crustimoney.combinators/grammar).

  A map of existing parsers can be supplied, which can be used by the
  string grammar. For example:

      (create-parser "root <- 'Hello ' email"
                     {:email (regex "...")})
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/string_grammar.clj#L210-L256">Source</a></sub></p>

## <a name="crustimoney.string-grammar/vector-tree">`vector-tree`</a><a name="crustimoney.string-grammar/vector-tree"></a>
``` clojure

(vector-tree text)
```

Low-level function which translates the string grammar into an
  intermediary vector-based representation. See
  [`crustimoney.vector-grammar`](#crustimoney.vector-grammar) for more on this format. This can be
  useful for debugging.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/string_grammar.clj#L199-L208">Source</a></sub></p>

-----
# <a name="crustimoney.vector-grammar">crustimoney.vector-grammar</a>


A basic vector-driven parser generator.




## <a name="crustimoney.vector-grammar/create-parser">`create-parser`</a><a name="crustimoney.vector-grammar/create-parser"></a>
``` clojure

(create-parser tree)
```

Create a parser based on a vector-driven combinator tree. For
  example:

      {:foobar [:chain [:ref :foo] [:ref :bar]]
       :foo    [:literal "foo"]
       :bar    [:with-name :bax
                [:choice [:literal "bar"]
                         [:literal "baz"]]]}

  Each vector is expanded into the combinator invocation, referenced
  by the first keyword. If the keyword does not have a namespace,
  [`crustimoney.combinators`](#crustimoney.combinators) is assumed. Maps are walked as well,
  wrapped in [`crustimoney.combinators/grammar`](#crustimoney.combinators/grammar). Other data is left
  as-is.

  This type of parser generator is not intended to be used directly,
  though you can. It is used as an intermediary format for other
  formats, such as the string-based and data-based grammars.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney/vector_grammar.clj#L28-L57">Source</a></sub></p>
