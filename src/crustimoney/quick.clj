(ns crustimoney.quick
  "A namespace for those quick \"I'd rather use something better than a
  regex\"-moments."
  (:require [crustimoney.built-ins :as built-ins]
            [crustimoney.combinators :as c]
            [crustimoney.core :as core]
            [crustimoney.data-grammar :as data-grammar]
            [crustimoney.results :as r]
            [crustimoney.string-grammar :as string-grammar]))

(defn parse
  "Quickly parse `text` using the string- or data parser `definition`.
  The definition cannot be recursive. The predefined parsers in the
  `built-ins` namespace are available.

  A success result is transformed such that the matched texts are
  directly available. For example:

      (crusti \"'alice' (' and ' (:who word))+ $\"
              \"alice and bob and eve\")

      => [nil {:start 0, :end 21}
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
      (r/success->texts text result identity))))
