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
- Virtual stack, preventing stack overflows
- Infinite **loop detection** (runtime)
- Missing rule references detection (compile time)

## Usage

FIXME

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
