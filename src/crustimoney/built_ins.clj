(ns crustimoney.built-ins
  "A collection of common parsers.

  A map called `all` is also defined, which contain all the parsers in
  this namespace. This can be used as an extra parameter to the
  `grammar` macro for example."
  (:refer-clojure :exclude [newline])
  (:require [crustimoney.combinators :as c]))

(def space
  "Parse one or more whitespace characters, including newlines."
  (c/with-error :expected-space
    (c/regex #"\s+")))

(def space?
  "Parse zero or more whitespace characters, including newlines."
  (c/regex #"\s*"))

(def blank
  "Parse one or more space or tab characters, not newlines."
  (c/with-error :expected-blank
    (c/regex #"[ \t]+")))

(def blank?
  "Parse zero or more space or tab characters, not newlines."
  (c/regex #"[ \t]*"))

(def newline
  "Parse a single newline (either \\r\\n or \\n)."
  (c/with-error :expected-newline
    (c/regex #"\r?\n")))

(def integer
  "Parse a number, possibly negative."
  (c/with-error :expected-integer
    (c/regex #"-?\d+")))

(def natural
  "Parse a number, zero or higher."
  (c/with-error :expected-natural-number
    (c/regex #"\d+")))

(def word
  "Parse an alphabetical word."
  (c/with-error :expected-word
    (c/regex #"[A-Za-z]+")))

(def dquote
  "Parse an double quoted string, allowing \\\" escapes."
  (c/with-error :expected-double-qoute-string
    (c/chain (c/literal "\"")
             (c/regex #"(\\\"|[^\"])*")
             (c/literal "\""))))

(def squote
  "Parse an single quoted string, allowing \\' escapes."
  (c/with-error :expected-single-qoute-string
    (c/chain (c/literal "'")
             (c/regex #"(\\'|[^'])*")
             (c/literal "'"))))

(def all
  "A map with the built-in parsers."
  (reduce-kv (fn [m k v]
               (cond-> m (fn? @v) (assoc (keyword k) @v)))
             {} (ns-publics *ns*)))
