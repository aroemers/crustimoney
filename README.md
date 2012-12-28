# PEG parser

A Clojure library for simple, unambigues parsing of structured text, based on [Parsing Expression Grammars](http://portal.acm.org/citation.cfm?doid=964001.964011).


## Install

Since there has not been an official release yet, one has to "build" the library yourself. This requires [leiningen 2](https://github.com/technomancy/leiningen). Type the following in a terminal, while in the root of this project.

```bash
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
 {:content
  {:sum
   [{:sum-op "+", :product {:value {:number "2"}}}
    {:sum-op "-", :product {:value {:number "3"}}}
    {:product
     [{:product-op "*", :value {:number "10"}}
      {:value {:number "15"}}]}]}}}
```

As one can see, the AST is a direct derivative of the parsing rules, except for the fact that recursive rules are nicely wrapped in a single vector, instead of being nested.


## Todo

* Extend this documentation and compare it with other Clojure PEG parsers.
* Improve the reporting of parse errors, instead of reporting all possible errors.
* Improve the readability of the source, by splitting some large functions.
* Add support for *, + and ? modifiers, by adding rule rewriting.


## License

Copyright © 2012 A. Roemers

Distributed under the Eclipse Public License.
