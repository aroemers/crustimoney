(ns crustimoney.parse
  "This namespace contains the functions that should be called by the users
  of this library. The main function is `parse`."
  (:require [crustimoney.internal.core :as core])
  (:use [crustimoney.internal.utils]
        [crustimoney.i18n :only (i18n)]))


;;; Private helper functions.

(defn- make-error
  "Create an error map based on the arguments."
  [errors line column pos]
  {:error (mapify errors line column pos)})


;;; Main functions.

(defn parse
  "Parse the given `text` string using the specified `rules` map, starting from
  the rule specified by the `start` keyword. See the documentation on how to
  create a rules map.

  This function returns either a map with either a `:succes` or an `:error` key
  in it. The value of the `:succes` key is the abstract syntax tree (AST). See
  the documentation on how this AST is stuctured.

  The value of the `:error` key is a map with the following keys:

  - `:errors` contains a set with possible parse errors.
  - `:line`   contains the line number of where the error(s) occured.
  - `:column` contains the column number of where the error(s) occured in the
              line.
  - `:pos`    contains the overall character position of where the error occured."
  [rules start text]
  (let [init-state (core/map->State {:rules rules
                                     :remainder text
                                     :pos 0
                                     :errors #{}
                                     :errors-pos 0})
        result (core/parse-nonterminal start init-state)]
    (if-let [succes (:succes result)]
      ;; Check whether all the text has been parsed.
      (if (empty? (get-in succes [:new-state :remainder]))
        {:succes (:content succes)}
        (let [errors (get-in succes [:new-state :errors])
              errors-pos (if (empty? errors)
                            (get-in succes [:new-state :pos])
                            (get-in succes [:new-state :errors-pos]))
              errors (if (empty? errors) #{(i18n :expected-eof)} errors)
              [line column] (core/line-and-column errors-pos text)]
          (make-error errors line column errors-pos)))
      (let [errors-pos (:errors-pos result)
            [line column] (core/line-and-column errors-pos text)]
        (make-error (:errors result) line column errors-pos)))))

(defn with-spaces
  "This function returns a vector with mandatory white-space between the
  specified items."
  [& items]
  (into [] (interpose #"\s+" items)))