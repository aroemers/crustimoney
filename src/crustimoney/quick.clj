(ns crustimoney.quick
  "A namespace for those quick \"I need something better than a
  regex\"-moments."
  (:require [crustimoney.built-ins :as built-ins]
            [crustimoney.combinators :as c]
            [crustimoney.core :as core]
            [crustimoney.data-grammar :as data-grammar]
            [crustimoney.results :as r]
            [crustimoney.string-grammar :as string-grammar]))

(defn- success->texts [text success]
  ((fn inner [success]
     (into [(r/success->name success) (r/success->text text success)]
           (map inner (r/success->children success))))
   success))

(defn parse
  "Quickly parse `text` using the string- or data parser `definition`.
  The definition cannot be recursive. The predefined parsers in the
  `built-ins` namespace are available.

  A success result is transformed such that the matched texts are
  directly available. For example:

      (parse \"'alice' (' and ' (:who word))+\"
             \"alice and bob and eve\")

      => [nil \"alice and bob and eve\"
          [:who \"bob\"]
          [:who \"eve\"]]

  When the result is an error, nil is returned."
  [definition text]
  (let [rules (c/grammar built-ins/all
                         {:root (if (string? definition)
                                  (string-grammar/create-parser definition)
                                  (data-grammar/create-parser definition))})
        result (core/parse (:root rules) text)]
    (when (r/success? result)
      (success->texts text result))))
