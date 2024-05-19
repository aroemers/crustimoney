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
    ((fn inner [node]
       (let [base     {(r/success->name node) (r/success->text node text)}
             children (r/success->children node)]
         (cond-> base (seq children) (assoc nil (map inner children)))))
     result)))

(defn parse
  "Quickly parse `text` using the string- or data parser `definition`.
  The predefined parsers in the `built-ins` namespace are available.

  A success result is transformed such that each node is a map, where
  the node's name contains the matched text and the `nil` key contains
  its children. For example:

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
