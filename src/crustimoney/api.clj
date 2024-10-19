(ns crustimoney.api
  "Contains aliases to common functions in other namespaces"
  (:refer-clojure :exclude [compile])
  (:require [crustimoney.built-ins :as built-ins]
            [crustimoney.core :as core]
            [crustimoney.data-grammar :as data-grammar]
            [crustimoney.results :as results]
            [crustimoney.string-grammar :as string-grammar]))

;;; Macro alias util

(defn- with-docs-of [from onto]
  (let [from-meta (select-keys (meta from) [:doc :arglists])]
    (alter-meta! onto merge from-meta)))

;;; Core aliases

(def parse core/parse)

(def compile core/compile)

;;; Grammar aliases

(def parser-from-data data-grammar/create-parser)

(def parser-from-string string-grammar/create-parser)

;;; Results aliases

(def transform results/transform)

(with-docs-of #'results/coerce
  (defmacro coerce [& args]
    `(results/coerce ~@args)))

(with-docs-of #'results/collect
  (defmacro collect [& args]
    `(results/collect ~@args)))

;;; Built-ins aliases

(def built-ins built-ins/all)
