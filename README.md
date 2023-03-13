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

## Usage

First, add this library to your dependencies.
The instructions for the latest version can be found here: [![Clojars Project](https://img.shields.io/clojars/v/functionalbytes/crustimoney.svg)](https://clojars.org/functionalbytes/crustimoney)

### The combinators

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

### Parse results

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

### Recursive grammars

Composing a single parser can be enough in some cases.
More complex texts need recursive grammar, i.e. named parsers that can refer to each other.
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

### Auto-named rules

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
Parsing it again, would yield the following result:

```clj
[:root {:start 0, :end 6}
 [:foo {:start 0, :end 3}]
 [:bax {:start 3, :end 6}]]
```

A word of caution though.
It is encouraged to be very intentional about which nodes should be captured and when.
For example, using the following grammar would only yield a `:wrapped` node if the `:expr` is really wrapped:

```clj
{:wrapped (choice (with-name :wrapped
                    (chain (literal "(")
                           (ref :expr)
                           (literal ")")))
                  (ref :expr))
 :expr    (literal "e")}
```


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
