# PEG parser

A Clojure library for simple, unambiguous parsing of structured text, based on [Parsing Expression Grammars](http://portal.acm.org/citation.cfm?doid=964001.964011).


## Adding it to your project

Since there has not been an official release yet, the library has to be "build" by yourself. This requires [leiningen 2](https://github.com/technomancy/leiningen) and [maven 3](https://maven.apache.org). Type the following commands in a terminal:

```bash
$ git clone git://github.com/aroemers/pegparser.git
$ cd pegparser
$ lein jar
$ mvn install:install-file -Dfile=target/pegparser-0.1.0-SNAPSHOT.jar -DgroupId=pegparser -DartifactId=pegparser -Dversion=0.1.0-SNAPSHOT -Dpackaging=jar
```

Now you can add `[pegparser 0.1.0-SNAPSHOT]` to the dependencies in your project.clj.


## Usage

The parsing rules are defined in a map. The key is a keyword denoting the rule's name and the value is the parsing expression. A parsing expression may be one of the following:

* A character, such as `\a`,
* A string, such as `"abc"`,
* A regular expression, such as `#"[a-c]+"`, or
* A vector, such as `[ \a :b "c" / #"[d-z]+" [ \+ / \- ] ]`.

The first three are considered terminals. A vector denotes a sequence, and may be nested. A vector may contain any of the terminals, but also keywords to refer to other rules and the choice operator `/`. Any terminal in the vector is parsed, but do not end up in the resulting AST. As is standard in PEG parsers, the choices are prioritized, i.e. the first succesfully parsed choice is used.

### Simple example

An example map of rules is the following:

```clojure
(def calc
  {:expr          [ :sum ]
   :sum           [ :product :sum-op :sum / :product ]
   :product       [ :value :product-op :product / :value ]
   :value         [ :number / \( :expr \) ]
   :sum-op        #"(?:\+|-)"
   :product-op    #"(?:\*|/)"
   :number        #"[0-9]+"})
```

Above rules can parse simple arithmatic. Calling the `pegparser.parse/parse` function with these rules and an expression would yield the following:

```clojure
=> (parse calc :expr "2+3-10*15")
{:succes
 {:sum
  [{:sum-op "+", :product {:value {:number "2"}}}
   {:sum-op "-", :product {:value {:number "3"}}}
   {:product
    [{:product-op "*", :value {:number "10"}}
     {:value {:number "15"}}]}]}}
```

As one can see, the AST is a direct derivative of the parsing rules, except for the fact that recursive rules are nicely wrapped in a single vector, instead of being nested.

Note that PEG parsers have "greedy" parsing expressions by definition (and again, choices are prioritized). This means the following two things:

* Expressions cannot be left recursive. For example a rule like `{:x [ :x \a / \b ]}` will never terminate. This is however a minor limitation, in return for very clear parsing semantics, since every expression can be rewritten to not being left recursive.
* Expressions like `{:x [ \a :x / \a ]}` will always fail, since all the `a` characters will be consumed by the first choice. This however can be rewritten by adding an extra rule: `{:x [ \a :y ]  :y [ \a :y / ]}`.

### Non-terminals as terminals

Sometimes one wants terminals that cannot be defined by standard regular expressions, e.g.  correctly nested parentheses. This can easily be defined using non-terminals, but this complicates the resulting AST. Therefore the library supports non-terminals that act like terminals. One achieves this by appending a `-` sign to the name of the rule. For example:

```clojure
(def nested
  {:root          [ :parens ]
   :parens-       [ :non-paren :parens / :paren-open :parens :paren-close :parens / ]
   :non-paren     #"[^\(\)]"
   :paren-open    \(
   :paren-close   \)})
```

Above rule map parses any text, as long as the parentheses match correctly. But more importantly, the `:parens` part is regarded as a terminal, as can be seen when one parses an arbitrary expression:

```clojure
=> (parse nested :root "((foo)bar(baz))woz")
{:succes {:parens "((foo)bar(baz))woz"}}
```

### Parse errors

In case one supplies an expression that cannot be parsed, the result is as follows:

```clojure
=> (parse calc :expr "2+3-10*") ; notice the missing part at the end.
{:error
  {:errors #{"expected character '('"
             "expected a character sequence that matches '[0-9]+'"},
   :line 1, :column 8, :pos 7}}

=> (parse nested :root "((foo)bar(") ; notice the last paren.
{:error
  {:errors #{"expected character ')'"
             "expected character '('"
             "expected a character sequence that matches '[^\\(\\)]'"},
   :line 1, :column 11, :pos 10}}
```

The `:errors` key contains a set of possible errors on the specified `:line` at the specified `:column`. The `:pos` key contains the overall character position of the errors in the text, starting at 0.

### Whitespace

Whitespace needs to be defined explicit in the grammar. The `pegparser.parse/with-spaces` function is a small helper function for sequences that have mandatory whitespace between the items. For example:

```clojure
=> (def hello
     {:hello (with-spaces "hello" :name)
      :name  #"[a-z]+"})

=> (parse hello :hello "hello  world")
{:succes {:name "world"}}

=> (parse hello :hello "helloworld")
{:error
  {:errors #{"expected a character sequence that matches '\\s+'"},
   :line 1, :column 6, :pos 5}}

=> (parse hello :hello "hello  world ") ; notice the space at the end.
{:error
  {:errors #{"expected EOF"},
   :line 1, :column 13, :pos 12}}
```


## Todo

* Extend this documentation and compare it with other Clojure PEG parsers.
* ~~Improve the reporting of parse errors, instead of reporting all possible errors.~~ Done!
* ~~Mention line and column number of parse errors, instead of the overall character position.~~ Done!
* Improve the readability of the source, by splitting some large functions and adding documentation.
* Add support for *, + and ? modifiers, by adding rule rewriting.
* Decide whether rules that might recurse always return a vector or not. Currently it does not (as can be seen by looking at the `:product` values in the `calc` example AST).
* Decide whether non-terminal rules that act like terminals (using the `-` sign) should also include the terminals _inside_ the vector in the parse result. Currently it does not, which is why the `nested` example needs explicit `:non-paren`, `:paren-open` and `:paren-close` rules.
* Add support for internationalization of error messages.
* Add a function that checks the grammar for issues like left-recursion.


## License

Copyright © 2012 A. Roemers

Distributed under the Eclipse Public License.
