# Table of contents
-  [`crustimoney2.caches`](#crustimoney2.caches)  - Packrat caches for the core/parse function.
    -  [`atom-cache`](#crustimoney2.caches/atom-cache) - Create a cache that uses a plain atom for storage, without any eviction (until it is garbage collected that is).
    -  [`clojure-core-cache`](#crustimoney2.caches/clojure-core-cache) - Create a cache by wrapping a clojure.core.cache cache.
-  [`crustimoney2.combinators`](#crustimoney2.combinators)  - Parsers combinator functions.
    -  [`chain`](#crustimoney2.combinators/chain) - Chain multiple consecutive parsers.
    -  [`choice`](#crustimoney2.combinators/choice) - Match the first of the ordered parsers that is successful.
    -  [`eof`](#crustimoney2.combinators/eof) - Succeed only if the entire text has been parsed.
    -  [`literal`](#crustimoney2.combinators/literal) - A parser that matches an exact literal string.
    -  [`lookahead`](#crustimoney2.combinators/lookahead) - Lookahead for the given parser, i.e.
    -  [`maybe`](#crustimoney2.combinators/maybe) - Try to parse the given parser, but succeed anyway.
    -  [`negate`](#crustimoney2.combinators/negate) - Negative lookahead for the given parser, i.e.
    -  [`regex`](#crustimoney2.combinators/regex) - A parser that matches the given regular expression.
    -  [`repeat*`](#crustimoney2.combinators/repeat*) - Eagerly try to match the given parser as many times as possible.
    -  [`repeat+`](#crustimoney2.combinators/repeat+) - Eagerly try to match the parser as many times as possible, expecting at least one match.
    -  [`with-error`](#crustimoney2.combinators/with-error) - Wrap the parser, replacing any errors with a single error with the supplied error key.
    -  [`with-name`](#crustimoney2.combinators/with-name) - Wrap the parser, assigning a name to the (success) result of the parser.
    -  [`with-value`](#crustimoney2.combinators/with-value) - Wrap the parser, adding a <code>:value</code> attribute to its success, containing the matched text.
-  [`crustimoney2.core`](#crustimoney2.core)  - The main parsing functions.
    -  [`parse`](#crustimoney2.core/parse) - Use the given parser to parse the supplied text string.
    -  [`ref`](#crustimoney2.core/ref) - Creates a parser function that wraps another parser function, which is referred to by the given key.
    -  [`rmap`](#crustimoney2.core/rmap) - Takes (something that evaluates to) a map, in which the entries can refer to each other using the <code>ref</code> function.
-  [`crustimoney2.data-grammar`](#crustimoney2.data-grammar)  - Create a parser based on a data grammar.
    -  [`create-parser`](#crustimoney2.data-grammar/create-parser) - Create a parser based on a data grammar definition.
    -  [`vector-tree-for`](#crustimoney2.data-grammar/vector-tree-for) - Low-level (multi method) function which translates the data grammar into an intermediary vector-based representation.
-  [`crustimoney2.results`](#crustimoney2.results)  - Result constructors, accessors and predicates.
    -  [`->error`](#crustimoney2.results/->error) - Create an error result, given an error key and an index.
    -  [`->push`](#crustimoney2.results/->push) - Create a push value, given a parser function and an index.
    -  [`->success`](#crustimoney2.results/->success) - Create a success result, given a start index (inclusive) and end index (exclusive).
    -  [`error->detail`](#crustimoney2.results/error->detail) - Return the detail object of an error.
    -  [`error->index`](#crustimoney2.results/error->index) - Return the index of an error.
    -  [`error->key`](#crustimoney2.results/error->key) - Return the key of an error.
    -  [`errors->line-column`](#crustimoney2.results/errors->line-column) - Adds <code>:line</code> and <code>:column</code> entries to each of the errors, in an efficient way.
    -  [`push->index`](#crustimoney2.results/push->index) - Returns the index of a push value.
    -  [`push->parser`](#crustimoney2.results/push->parser) - Returns the parser of a push value.
    -  [`push->state`](#crustimoney2.results/push->state) - Returns the state of a push value.
    -  [`push?`](#crustimoney2.results/push?) - Returns obj if obj is a push value.
    -  [`success->attr`](#crustimoney2.results/success->attr) - Returns an attribute value of a success.
    -  [`success->attrs`](#crustimoney2.results/success->attrs) - Return the attributes of a success.
    -  [`success->children`](#crustimoney2.results/success->children) - Returns the children of a success.
    -  [`success->end`](#crustimoney2.results/success->end) - Return the end index of a success.
    -  [`success->name`](#crustimoney2.results/success->name) - Return the name of a success.
    -  [`success->start`](#crustimoney2.results/success->start) - Return the start index of a success.
    -  [`success->text`](#crustimoney2.results/success->text) - Returns the matched text of a success, given the full text.
    -  [`success?`](#crustimoney2.results/success?) - Returns obj if obj is a success value, nil otherwise.
    -  [`with-success-attrs`](#crustimoney2.results/with-success-attrs) - Add extra success attributes to the given success.
    -  [`with-success-children`](#crustimoney2.results/with-success-children) - Set the children of a success.
    -  [`with-success-name`](#crustimoney2.results/with-success-name) - Set the name of the success value.
-  [`crustimoney2.string-grammar`](#crustimoney2.string-grammar)  - Create a parser based on a string grammar.
    -  [`create-parser`](#crustimoney2.string-grammar/create-parser) - Create a parser based on a string-based grammar definition.
    -  [`vector-tree`](#crustimoney2.string-grammar/vector-tree) - Low-level function which translates the string grammar into an intermediary vector-based representation.
-  [`crustimoney2.vector-grammar`](#crustimoney2.vector-grammar)  - A basic vector-driven parser generator.
    -  [`create-parser`](#crustimoney2.vector-grammar/create-parser) - Create a parser based on a vector-driven combinator tree.

-----
# <a name="crustimoney2.caches">crustimoney2.caches</a>


Packrat caches for the core/parse function.

  Caches are implemented by functions that take three and four
  arguments. The function is called with three arguments (text,
  parser, index) to try and fetch a cached result. It returns nil if
  it misses the cache. The function is called with four
  arguments (text, parser, index, result) to store a result. It
  returns the result back.




## <a name="crustimoney2.caches/atom-cache">`atom-cache`</a><a name="crustimoney2.caches/atom-cache"></a>
``` clojure

(atom-cache)
```

Create a cache that uses a plain atom for storage, without any
  eviction (until it is garbage collected that is).
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/caches.clj#L11-L21">Source</a></sub></p>

## <a name="crustimoney2.caches/clojure-core-cache">`clojure-core-cache`</a><a name="crustimoney2.caches/clojure-core-cache"></a>
``` clojure

(clojure-core-cache cache)
```

Create a cache by wrapping a clojure.core.cache cache.

  This function resolves the clojure.core.cache library dynamically;
  you'll need to add the dependency to it yourself.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/caches.clj#L23-L37">Source</a></sub></p>

-----
# <a name="crustimoney2.combinators">crustimoney2.combinators</a>


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

  Both arities can return a success, a list of errors, or a push. The
  [`crustimoney2.results`](#crustimoney2.results) namespace should be used for creating and
  reading these results.

  Before you write your own combinator, do realise that the provided
  combinators are complete in the sense that they can parse any text.




## <a name="crustimoney2.combinators/chain">`chain`</a><a name="crustimoney2.combinators/chain"></a>
``` clojure

(chain & parsers)
```

Chain multiple consecutive parsers.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/combinators.clj#L51-L68">Source</a></sub></p>

## <a name="crustimoney2.combinators/choice">`choice`</a><a name="crustimoney2.combinators/choice"></a>
``` clojure

(choice & parsers)
```

Match the first of the ordered parsers that is successful.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/combinators.clj#L70-L85">Source</a></sub></p>

## <a name="crustimoney2.combinators/eof">`eof`</a><a name="crustimoney2.combinators/eof"></a>
``` clojure

(eof text index)
(eof parser)
```

Succeed only if the entire text has been parsed. Optionally another
  parser can be wrapped, after which the check is done when that parser
  is done (successfully). This means that `(chain a-parser eof)` behaves
  the same as `(eof a-parser)`, though the latter form evaluates to the
  result of the wrapped parser, whereas the former eof creates its own
  (empty) success.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/combinators.clj#L164-L186">Source</a></sub></p>

## <a name="crustimoney2.combinators/literal">`literal`</a><a name="crustimoney2.combinators/literal"></a>
``` clojure

(literal s)
```

A parser that matches an exact literal string.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/combinators.clj#L42-L49">Source</a></sub></p>

## <a name="crustimoney2.combinators/lookahead">`lookahead`</a><a name="crustimoney2.combinators/lookahead"></a>
``` clojure

(lookahead parser)
```

Lookahead for the given parser, i.e. succeed if the parser does,
  without advancing the parsing position.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/combinators.clj#L140-L151">Source</a></sub></p>

## <a name="crustimoney2.combinators/maybe">`maybe`</a><a name="crustimoney2.combinators/maybe"></a>
``` clojure

(maybe parser)
```

Try to parse the given parser, but succeed anyway.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/combinators.clj#L153-L162">Source</a></sub></p>

## <a name="crustimoney2.combinators/negate">`negate`</a><a name="crustimoney2.combinators/negate"></a>
``` clojure

(negate parser)
```

Negative lookahead for the given parser, i.e. this succeeds if the
  parser does not.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/combinators.clj#L101-L112">Source</a></sub></p>

## <a name="crustimoney2.combinators/regex">`regex`</a><a name="crustimoney2.combinators/regex"></a>
``` clojure

(regex re)
```

A parser that matches the given regular expression.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/combinators.clj#L116-L123">Source</a></sub></p>

## <a name="crustimoney2.combinators/repeat*">`repeat*`</a><a name="crustimoney2.combinators/repeat*"></a>
``` clojure

(repeat* parser)
```

Eagerly try to match the given parser as many times as possible.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/combinators.clj#L87-L99">Source</a></sub></p>

## <a name="crustimoney2.combinators/repeat+">`repeat+`</a><a name="crustimoney2.combinators/repeat+"></a>
``` clojure

(repeat+ parser)
```

Eagerly try to match the parser as many times as possible, expecting
  at least one match.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/combinators.clj#L125-L138">Source</a></sub></p>

## <a name="crustimoney2.combinators/with-error">`with-error`</a><a name="crustimoney2.combinators/with-error"></a>
``` clojure

(with-error key parser)
```

Wrap the parser, replacing any errors with a single error with the
  supplied error key.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/combinators.clj#L199-L207">Source</a></sub></p>

## <a name="crustimoney2.combinators/with-name">`with-name`</a><a name="crustimoney2.combinators/with-name"></a>
``` clojure

(with-name key parser)
```

Wrap the parser, assigning a name to the (success) result of the
  parser. Nameless parsers are filtered out by default during
  parsing.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/combinators.clj#L190-L197">Source</a></sub></p>

## <a name="crustimoney2.combinators/with-value">`with-value`</a><a name="crustimoney2.combinators/with-value"></a>
``` clojure

(with-value parser)
(with-value f parser)
```

Wrap the parser, adding a `:value` attribute to its success,
  containing the matched text. Optionally takes a function f, applied
  to the text value.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/combinators.clj#L209-L220">Source</a></sub></p>

-----
# <a name="crustimoney2.core">crustimoney2.core</a>


The main parsing functions.




## <a name="crustimoney2.core/parse">`parse`</a><a name="crustimoney2.core/parse"></a>
``` clojure

(parse parser text)
(parse parser text opts)
```

Use the given parser to parse the supplied text string. The result
  will either be a success (a hiccup-style vector) or a list of
  errors. By default only named nodes are kept in a success
  result (the root node is allowed to be nameless).

  A success result looks like this:

    [:name {:start 0, :end 3}
     [:child-1 {:start 0, :end 2, :value "aa"}]
     [:child-2 {:start 2, :end 3}]]

  An error result looks like this:

    ({:key :failed-lookahead, :at 0}
     {:key :expected-literal, :at 0, :detail {:literal "foo"}})

  The parse function can take an options map, with the following
  options:

  `:index` - the index at which to start parsing in the text, default 0.

  `:cache` - the packrat caching function to use, see the caching
  namespaces, default nil.

  `:keep-nameless` - set this to true if nameless success nodes should
  be kept in the parse result, for debugging, defaults to false.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/core.clj#L21-L85">Source</a></sub></p>

## <a name="crustimoney2.core/ref">`ref`</a><a name="crustimoney2.core/ref"></a>
``` clojure

(ref key)
```

Creates a parser function that wraps another parser function, which
  is referred to by the given key. Needs to be called within the
  lexical scope of [`rmap`](#crustimoney2.core/rmap).
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/core.clj#L91-L101">Source</a></sub></p>

## <a name="crustimoney2.core/rmap">`rmap`</a><a name="crustimoney2.core/rmap"></a>
``` clojure

(rmap grammar)
```
Function.

Takes (something that evaluates to) a map, in which the entries can
  refer to each other using the [`ref`](#crustimoney2.core/ref) function. In other words, a
  recursive map. For example:

  (rmap {:foo  (literal "foo")
         :root (chain (ref :foo) "bar")})
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/core.clj#L110-L118">Source</a></sub></p>

-----
# <a name="crustimoney2.data-grammar">crustimoney2.data-grammar</a>


Create a parser based on a data grammar. The data is translated into
  combinators.




## <a name="crustimoney2.data-grammar/create-parser">`create-parser`</a><a name="crustimoney2.data-grammar/create-parser"></a>
``` clojure

(create-parser data)
(create-parser data other-parsers)
```

Create a parser based on a data grammar definition. If a map with
  rules is supplied, a map of parsers is returned. Otherwise a single
  parser is returned. Optionally an existing map of parsers can be
  supplied, which can refered to by the data grammar. The following
  example shows what a data grammar looks like:

    {;; terminals
     literal            "foo"
     character          \c
     regex              #"[a-z]"
     eof                $

     ;; refs and grouping
     reference          literal
     chain              (literal regex)
     choices            (literal / regex / "alice" "bob")
     named-group        (:my-name literal / "the end" $)

     ;; quantifiers
     zero-to-many       (literal *)
     one-to-many        ("bar"+)
     zero-to-one        ("foo" "bar"?) ; bar is optional here

     ;; lookaheads
     lookahead          (& regex)
     negative-lookahead (!"alice")

     ;; direct combinator calls
     combinator-call       [:with-value (:bax "bar" / "baz")]
     combinator-plain-data [:with-error #crust/plain :fail! "foo"]
     custom-combinator     [:my.app/my-combinator literal]}

  To capture nodes in the parse result, you need to use named groups.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/data_grammar.clj#L101-L140">Source</a></sub></p>

## <a name="crustimoney2.data-grammar/vector-tree-for">`vector-tree-for`</a><a name="crustimoney2.data-grammar/vector-tree-for"></a>




Low-level (multi method) function which translates the data grammar
  into an intermediary vector-based representation. See
  [`crustimoney2.vector-grammar`](#crustimoney2.vector-grammar) for more on this format. This can be
  useful for debugging, or adding your own data type.

  In the latter case, add your type like so:

    (defmethod vector-tree-for java.util.Date [date]
      [:my-namespace/my-flexible-date-parser date])

  To see which data types are already supported, use `(methods
  vector-tree-for)`
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/data_grammar.clj#L14-L28">Source</a></sub></p>

-----
# <a name="crustimoney2.results">crustimoney2.results</a>


Result constructors, accessors and predicates




## <a name="crustimoney2.results/->error">`->error`</a><a name="crustimoney2.results/->error"></a>
``` clojure

(->error key index)
(->error key index detail)
```

Create an error result, given an error key and an index. An extra
  detail object can be added.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/results.clj#L74-L80">Source</a></sub></p>

## <a name="crustimoney2.results/->push">`->push`</a><a name="crustimoney2.results/->push"></a>
``` clojure

(->push parser index)
(->push parser index state)
```

Create a push value, given a parser function and an index. Optionally
  a state object can be added.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/results.clj#L99-L105">Source</a></sub></p>

## <a name="crustimoney2.results/->success">`->success`</a><a name="crustimoney2.results/->success"></a>
``` clojure

(->success start end)
(->success start end children)
```

Create a success result, given a start index (inclusive) and end
  index (exclusive). Optionally a collection of success children can
  be given. The name of the success is nil.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/results.clj#L6-L13">Source</a></sub></p>

## <a name="crustimoney2.results/error->detail">`error->detail`</a><a name="crustimoney2.results/error->detail"></a>
``` clojure

(error->detail error)
```

Return the detail object of an error.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/results.clj#L92-L95">Source</a></sub></p>

## <a name="crustimoney2.results/error->index">`error->index`</a><a name="crustimoney2.results/error->index"></a>
``` clojure

(error->index error)
```

Return the index of an error
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/results.clj#L87-L90">Source</a></sub></p>

## <a name="crustimoney2.results/error->key">`error->key`</a><a name="crustimoney2.results/error->key"></a>
``` clojure

(error->key error)
```

Return the key of an error.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/results.clj#L82-L85">Source</a></sub></p>

## <a name="crustimoney2.results/errors->line-column">`errors->line-column`</a><a name="crustimoney2.results/errors->line-column"></a>
``` clojure

(errors->line-column errors text)
```

Adds `:line` and `:column` entries to each of the errors, in an
  efficient way.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/results.clj#L151-L160">Source</a></sub></p>

## <a name="crustimoney2.results/push->index">`push->index`</a><a name="crustimoney2.results/push->index"></a>
``` clojure

(push->index push)
```

Returns the index of a push value.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/results.clj#L118-L121">Source</a></sub></p>

## <a name="crustimoney2.results/push->parser">`push->parser`</a><a name="crustimoney2.results/push->parser"></a>
``` clojure

(push->parser push)
```

Returns the parser of a push value.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/results.clj#L113-L116">Source</a></sub></p>

## <a name="crustimoney2.results/push->state">`push->state`</a><a name="crustimoney2.results/push->state"></a>
``` clojure

(push->state push)
```

Returns the state of a push value.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/results.clj#L123-L126">Source</a></sub></p>

## <a name="crustimoney2.results/push?">`push?`</a><a name="crustimoney2.results/push?"></a>
``` clojure

(push? obj)
```

Returns obj if obj is a push value.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/results.clj#L107-L111">Source</a></sub></p>

## <a name="crustimoney2.results/success->attr">`success->attr`</a><a name="crustimoney2.results/success->attr"></a>
``` clojure

(success->attr success attr)
```

Returns an attribute value of a success.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/results.clj#L62-L65">Source</a></sub></p>

## <a name="crustimoney2.results/success->attrs">`success->attrs`</a><a name="crustimoney2.results/success->attrs"></a>
``` clojure

(success->attrs success)
```

Return the attributes of a success.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/results.clj#L57-L60">Source</a></sub></p>

## <a name="crustimoney2.results/success->children">`success->children`</a><a name="crustimoney2.results/success->children"></a>
``` clojure

(success->children success)
```

Returns the children of a success.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/results.clj#L37-L40">Source</a></sub></p>

## <a name="crustimoney2.results/success->end">`success->end`</a><a name="crustimoney2.results/success->end"></a>
``` clojure

(success->end success)
```

Return the end index of a success.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/results.clj#L26-L29">Source</a></sub></p>

## <a name="crustimoney2.results/success->name">`success->name`</a><a name="crustimoney2.results/success->name"></a>
``` clojure

(success->name success)
```

Return the name of a success.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/results.clj#L47-L50">Source</a></sub></p>

## <a name="crustimoney2.results/success->start">`success->start`</a><a name="crustimoney2.results/success->start"></a>
``` clojure

(success->start success)
```

Return the start index of a success.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/results.clj#L21-L24">Source</a></sub></p>

## <a name="crustimoney2.results/success->text">`success->text`</a><a name="crustimoney2.results/success->text"></a>
``` clojure

(success->text success text)
```

Returns the matched text of a success, given the full text.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/results.clj#L67-L70">Source</a></sub></p>

## <a name="crustimoney2.results/success?">`success?`</a><a name="crustimoney2.results/success?"></a>
``` clojure

(success? obj)
```

Returns obj if obj is a success value, nil otherwise.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/results.clj#L15-L19">Source</a></sub></p>

## <a name="crustimoney2.results/with-success-attrs">`with-success-attrs`</a><a name="crustimoney2.results/with-success-attrs"></a>
``` clojure

(with-success-attrs success attrs)
```

Add extra success attributes to the given success.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/results.clj#L52-L55">Source</a></sub></p>

## <a name="crustimoney2.results/with-success-children">`with-success-children`</a><a name="crustimoney2.results/with-success-children"></a>
``` clojure

(with-success-children success children)
```

Set the children of a success.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/results.clj#L31-L35">Source</a></sub></p>

## <a name="crustimoney2.results/with-success-name">`with-success-name`</a><a name="crustimoney2.results/with-success-name"></a>
``` clojure

(with-success-name key success)
```

Set the name of the success value.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/results.clj#L42-L45">Source</a></sub></p>

-----
# <a name="crustimoney2.string-grammar">crustimoney2.string-grammar</a>


Create a parser based on a string grammar. The grammar is translated
  into combinators.




## <a name="crustimoney2.string-grammar/create-parser">`create-parser`</a><a name="crustimoney2.string-grammar/create-parser"></a>
``` clojure

(create-parser text)
(create-parser text other-parsers)
```

Create a parser based on a string-based grammar definition. If the
  definition contains multiple rules, a map of parsers is returned.
  Optionally an existing map of parsers can be supplied, which can be
  used by the string grammar. The following definition describes the
  string grammar syntax in itself:

    space           <- [ 	]*
    whitespace      <- [\s]*

    non-terminal    <- (:non-terminal [a-zA-Z_-]+)
    literal         <- '''' (:literal ('''''' / [^'])*) ''''
    character-class <- (:character-class '[' (']]' / [^]]])* ']')
    end-of-file     <- (:end-of-file '$')

    group-name      <- ':' (:group-name [a-zA-Z_-]+)
    group           <- (:group '(' group-name? space choice space ')')

    expr            <- non-terminal / group / literal / character-class / end-of-file

    quantified      <- (:quantified expr (:operand [?+*])) / expr
    lookahead       <- (:lookahead (:operand [&!]) quantified) / quantified

    chain           <- (:chain lookahead (space lookahead)+) / lookahead
    choice          <- (:choice chain (space '/' space chain)+) / chain

    rule            <- (:rule (:rule-name non-terminal) space '<-' space choice)
    rules           <- (:rules (whitespace rule whitespace)+)
    no-rules        <- (:no-rules whitespace choice whitespace)
    root            <- (:root rules / no-rules) $

  To capture nodes in the parse result, you need to use named groups.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/string_grammar.clj#L195-L232">Source</a></sub></p>

## <a name="crustimoney2.string-grammar/vector-tree">`vector-tree`</a><a name="crustimoney2.string-grammar/vector-tree"></a>
``` clojure

(vector-tree text)
```

Low-level function which translates the string grammar into an
  intermediary vector-based representation. See
  [`crustimoney2.vector-grammar`](#crustimoney2.vector-grammar) for more on this format. This can be
  useful for debugging.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/string_grammar.clj#L184-L193">Source</a></sub></p>

-----
# <a name="crustimoney2.vector-grammar">crustimoney2.vector-grammar</a>


A basic vector-driven parser generator.




## <a name="crustimoney2.vector-grammar/create-parser">`create-parser`</a><a name="crustimoney2.vector-grammar/create-parser"></a>
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
  [`crustimoney2.combinators`](#crustimoney2.combinators) is assumed. Maps are walked as well,
  wrapped in [`crustimoney2.core/rmap`](#crustimoney2.core/rmap). Other data is left as-is.

  This type of parser generator is not intended to be used directly,
  though you can. It is used as an intermediary format for other
  formats, such as the string-based and data-based grammars.
<p><sub><a href="https://github.com/aroemers/crustimoney/blob/v2/src/crustimoney2/vector_grammar.clj#L33-L61">Source</a></sub></p>
