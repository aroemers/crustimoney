(ns crustimoney.quick
  "A namespace for those quick \"I need something better than a
  regex\"-moments."
  (:require [crustimoney.built-ins :as built-ins]
            [crustimoney.core :as core]
            [crustimoney.data-grammar :as dg]
            [crustimoney.results :as r]
            [crustimoney.string-grammar :as sg]))

(defn transform
  "Transform a success result to the 'quick' format, nil otherwise."
  [result text]
  (when (r/success? result)
    (r/postwalk (fn [node] (assoc node 1 (r/success->text node text))) result)))

(defn parse
  "Parse `text` using the string- or data parser `definition`.
  The predefined parsers from the `built-ins` namespace are available.

  A success result is transformed such that the attributes of each
  node are replaced by the matched string. For example:

      (parse \"'alice' (' and ' (:who word))+\"
             \"alice and bob and eve\")

      => [nil \"alice and bob and eve\"
          [:who \"bob\"]
          [:who \"eve\"]]

  When the result is an error, nil is returned."
  [definition text]
  (let [rules (merge built-ins/all
                     (let [result (if (string? definition)
                                    (sg/create-parser definition)
                                    (dg/create-parser definition))]
                       (if (map? result) result {:root result})))
        result (core/parse rules text)]
    (transform result text)))
