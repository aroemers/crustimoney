[![Clojars Project](https://img.shields.io/clojars/v/functionalbytes/crustimoney.svg)](https://clojars.org/functionalbytes/crustimoney)
[![cljdoc badge](https://cljdoc.org/badge/functionalbytes/crustimoney)](https://cljdoc.org/d/functionalbytes/crustimoney/CURRENT)
[![Clojure CI](https://github.com/aroemers/crustimoney/workflows/Clojure%20CI/badge.svg?branch=master)](https://github.com/aroemers/crustimoney/actions?query=workflow%3A%22Clojure+CI%22)
[![Clojars Project](https://img.shields.io/clojars/dt/functionalbytes/crustimoney?color=blue)](https://clojars.org/functionalbytes/crustimoney)
[![Blogpost](https://img.shields.io/badge/blog-Crustimoney%202.0-blue)](https://functionalbytes.nl/clojure/crustimoney/2023/03/03/crustimoney20.html)

# 📙 crustimoney

A Clojure library for PEG parsing, supporting various grammars, packrat caching and cuts.

![Banner](banner.png)

## Motivation

Version 1 of crustimoney was my first library in Clojure, a long time ago.
Simply put, this version is the mental excercise of making it better.
I like to think it turned out well.
Maybe you like it too.

## Features

- Create parsers from **combinator** functions
- .. or from **string-based** definitions
- .. or from **data-driven** definitions
- Packrat **caching**, optimizing cpu usage
- Concept of **cuts**, optimizing memory usage - and error messages
- Focus on capture groups, resulting in **lean parse trees**, less coupled to grammar structure
- **Minimal parse tree** data, fetch only whats needed at post-processing
- Virtual stack, preventing overflows
- Infinite **loop detection** (runtime)
- Missing rule references detection (compile time)

## Add to project

First, add this library to your dependencies.
The instructions for the latest version can be found here: [![Clojars Project](https://img.shields.io/clojars/v/functionalbytes/crustimoney.svg)](https://clojars.org/functionalbytes/crustimoney)

## The combinators

The combinators are at the heart of the library.
Even if you decide to never use them directly, it is a good starting point.
Below is a list of available combinators.

The essentials:

- `literal`, match an exact literal string
- `chain`, chain multiple consecutive parsers
- `choice`, match the first successful parsers
- `repeat*`, eagerly match a parser as many times as possible
- `negate`, succeed if the given parser does not

Those are actually enough for parsing any unambiguous text.
But more combinators are provided, for ease of use, nicer result trees and better performance:

- `regex`, match a regular expression
- `repeat+`, same as `repeat*`, but require at least one match
- `lookahead`, succeed if the given parser does, without advancing
- `maybe`, try the given parser, succeed anyway
- `eof`, succeed if there is no more input

Each combinator returns a parser.
Some combinators take one or more parsers, making them composable.
Such a parser can be supplied to `core/parse`, together with the string to parse.

## Parse results

The result is a "hiccup"-style parse tree, for example:

```clj
[:node {:start 0, :end 6}
 [:child-node {:start 0, :end 3}]
 [:child-node {:start 3, :end 6}]]
```

To capture a node during parsing, it must be "named", such as `:node` or `:child-node` in above example.
This is done by wrapping a parser with `combinators/with-name` (or by other means, depending on the grammar type).
Results without a name are filtered out, though its named children are kept.
The root node can be nameless (`nil`).

On unsuccessful parses, a set of errors is returned, which has the following structure:

```clj
#{{:key :expected-literal, :at 10, :detail {:literal "foo"}}
  {:key :expected-match, :at 8, :detail {:regex "alice|bob"}}
  {:key :unexpected-match, :at 8, :detail {:text "eve"}}
  {:key :failed-lookahead, :at 10}}
```

If you want to override the default key of an error, a parser can be wrapped with `combinators/with-error`.
For example:

```clj
(def parser
  (with-error :number-required
    (regex #"\d+")))

(core/parse parser "nan")
:=> #{{:key :number-required, :at 0}}
```

To work with these successes and errorss, the functions in the `results` namespace can be used.
These allow you to get the text of a success node for example, or add `:line` and `:column` keys to the errors.

## Recursive grammars

Composing a single parser can be enough in some cases.
More complex texts need or are better expressed with a recursive grammar, i.e. named parsers that can refer to each other.
For this the `grammar` macro and `ref` function is used.
For example:

```clj
(def my-grammar
  (grammar
   {:root (repeat+ (choice (ref :foo) (ref :bax)))
    :foo  (literal "foo")
    :bax  (regex "ba(r|z)")}))
```

This will return a normal map, where the refs have been bound to the rules in the grammar.
The macro will ensure that all references resolve correctly.
This grammar can be used as follows:

```clj
(core/parse (:root my-grammar) "foobaz")
:=> [nil {:start 0, :end 6}]
```

## Auto-named rules

The example above shows that all success nodes are filtered out, except the root node, as they are nameless.
The parsers could be wrapped with `with-name`, but the names can be the same as the grammar rule names in this case.
Appending an `=` to the rule name will automatically wrap the parser as such.
This would update the grammar to:

```clj
(grammar
 {:root= (repeat+ (choice (ref :foo) (ref :bax)))
  :foo=  (literal "foo")
  :bax=  (regex "ba(r|z)")})
```

Note that the `ref` keys are still without the postfix.
Parsing it again would yield the following result:

```clj
[:root {:start 0, :end 6}
 [:foo {:start 0, :end 3}]
 [:bax {:start 3, :end 6}]]
```

A word of caution though.
It is encouraged to be very intentional about which nodes should be captured and when.
For example, using the following grammar would _only_ yield a `:wrapped` node if the `:expr` is really wrapped in parentheses:

```clj
{:wrapped (choice (with-name :wrapped
                    (chain (literal "(")
                           (ref :expr)
                           (literal ")")))
                  (ref :expr))
 :expr=   (literal "e")}
```

This approach results in shallower result trees and thus less post-processing.

## Cuts

Most PEG parsers share a downside: they are memory hungry.
This is due to their packrat caching, that provides one of their upsides: linear parsing time.

[This paper](https://www.researchgate.net/publication/221292729_Packrat_parsers_can_handle_practical_grammars_in_mostly_constant_space) describes adding _cuts_ to PEGs, a concept that is known from Prolog.
Crustimoney expands on this by differentiating between _hard_ cuts and _soft_ cuts.

### Hard cuts

A hard cut tells the parser that it should never backtrack beyond the position where it encountered a hard cut.
This has two major benefits.
The first is better and more localized error messages.
The following example shows this, and also how to add a hard cut in the `chain` combinator.

```clj
(def example
  (maybe (chain (literal "[")
                :hard-cut
                (regex #"\d+")
                (literal "]"))))

(core/parse example "[")
;=> #{{:key :expected-match, :at 1, :detail {:regex #"\d"}}}
```

Without the hard cut, the parse would be successful (because of the `maybe` combinator).
But, since the text clearly opens a bracket, it would be better to fail.
The hard cut enforces this, as the mismatched `regex` error cannot backtrack.
So from a user's standpoint, a cut can already very beneficial.

The second major benefit is that the parser can release everything in its cache before the cut position.
It will never need this again.
This behaviour makes that well placed hard cuts - especially when parsing repeating structures - can alleviate the memory requirements to be constant.

Note that a cut can only be used within a `chain`, and never as the first element.
The preceding parser(s) should consume some input, and that input should only be valid for that chain of parser(s) at that point.

### Soft cuts

There are situations that localized error messages are desired, but backtracking should still be possible.
For such situations a soft cut can be used.
Such a cut also disallows backtracking, but only while inside the `chain`.
I.e. once the chain is successfully parsed, the soft cut has no effect anymore.

Consider the expansion of the previous example:

```clj
(def example
  (grammar
   {:prefix= (maybe (chain (literal "[")
                    :soft-cut
                    (regex #"\d+")
                    (literal "]")))
    :expr=   (choice (with-name :foo
                       (chain (ref :prefix)
                              (literal "foo")))
                     (with-name :bax
                       (chain (ref :prefix)
                              (regex #"ba(r|z)"))))}))

(core/parse (:expr example) "[")
;=> #{{:key :expected-match, :at 1, :detail {:regex #"\d"}}}

(core/parse (:expr example) "[25]baz")
;=> [:expr {:start 0, :end 7}
;    [:bax {:start 0, :end 7}
;     [:prefix {:start 0, :end 4}]]]
```

The `:hard-cut` has been replaced with a `:soft-cut`.
As shown, this still shows a localized error for the unfinished `:prefix`, yet it also allows backtracking to the `:bax` choice.

Since backtracking before the soft cut is still allowed outside of the chain's scope, the cache is not affected.
However, soft and hard cuts can be combined in a grammar.
We could for instance add another rule to our example grammar:

```clj
:root (repeat+ (chain (ref :expr) :hard-cut))
```

This effectively says that after each finished `:expr`, we won't backtrack, that part is done.
Many of such consecutive `:expr`s can be parsed, without memory requirements growing (except for the growing parse result tree of course).

The significance of cuts in PEGs must not be underestimated.
Try to use them in your grammar on somewhat larger inputs.
The overhead is small, and is actually countered by faster cache lookups.

## String-based grammar

A parser or grammar can be defined in a string.
While direct combinators have the most flexibility, a string-based definition is far denser.
The discussed combinators translate to this string-based grammar in the following way:

```
literal   <- 'foo'
chain     <- 'foo' 'bar'
choice    <- 'bar' / 'baz'

repeat*   <- 'foo'*
repeat+   <- 'foo'+
maybe     <- 'foo'?

negate    <- !'foo'
lookahead <- &'foo'

regex     <- #'ba(r|z)'
chars     <- [a-zA-Z]*

eof       <- $
ref       <- literal

group     <- ('foo' 'bar' / 'alice')
named     <- (:bax regex)

soft-cut  <- ('(' > expr? ')')
hard-cut  <- ('(' > expr? ')' >>)
```

The function `string-grammar/create-parser` is used to create a parser out of such a string.
Note that above "example" has rules and thus describes a recursive grammar.
Therefore a map is returned by `create-parser`.
Howover, it is perfectly valid to define a single parser, such as:

```
'alice and ' !'eve' [a-z]+
```

The names of the rules can have an `=` sign appended, for the auto-named feature discussed earlier.

Note (for the purists) that the `.` (dot, match any non-newline char) and `ε` (epsilon, match the empty string) from the original PEG paper are missing.
This is on purpose.
The other available constructs, such as regular expression support, have far better performance characteristics and nicer result trees.

## Data-based grammar

Next to the string-based definition, there is also a data-driven variant available.
The grammar below shows how such a definition is formed.
It is very similar to the string-based grammar.

```clj
'{literal    "foo"
  character  \f
  regex      #"ba(r|z)"
  data-regex #crust/regex "ba(r|z)" ; EDN support

  chain      ("foo "bar")
  choice     ("bar" / "baz")

  repeat*    ("foo"*)
  repeat+    ("foo"+)
  maybe      ("foo"?)

  negate     (!"foo")
  lookahead  (&"foo")

  eof        $
  ref        literal

  group      ("foo" "bar" / "alice")
  named      (:bax regex)

  soft-cut   ("(" > expr? ")")
  hard-cut   ("(" > expr? ")" >>)

  combinator-call   [:with-error :fail!
                     #crust/parser ("fooba" #"r|z")]
  custom-combinator [:my.app/my-combinator ...]}
```

The function `data-grammar/create-parser` is used to create a parser out of such a definition.

The data-based definition is quite similar to the string-based one.
It works the same way in supporting both recursive and non-recursive parsers, and also has auto-naming (the `=` postfix).

It does have an extra feature: direct combinator calls, using vectors.
The first keyword in the vector determines the combinator.
For keywords without a namespace, `crustimoney2.combinators` is assumed.
The other arguments are left as-is, except those tagged with `#crust/parser`.
With that tag, the data is processed again as aparser definition.

## Vector-based grammar

The former section on data-based grammar definitions described that a vector is a valid data type.
That means that it is possible to write the entire grammar using vectors.
Thing is, _this is actually what both the string-based and data-based parser generators do_.

...

## Writing your own combinator

...

## License

Copyright © 2022-2023 Arnout Roemers

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
