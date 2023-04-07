(ns crustimoney.quick
  (:require [crustimoney.core :as core]
            [crustimoney.data-grammar :as data-grammar]
            [crustimoney.results :as r]
            [crustimoney.string-grammar :as string-grammar]))

(defn parse
  "Quickly parse `text` using the string or data parser `definition`.
  A success result is transformed such that the matched texts are
  directly available. For example:

      (crusti \"'alice' (' and ' (:who [a-z]+))+\"
              \"alice and bob and eve\")

      => [nil \"alice and bob and eve\"
          [:who \"bob\"]
          [:who \"eve\"]]"
  [definition text]
  (let [parser (if (string? definition)
                 (string-grammar/create-parser definition)
                 (data-grammar/create-parser definition))
        result (core/parse parser text)]
    (cond->> result (r/success? result) (r/success->texts text))))
