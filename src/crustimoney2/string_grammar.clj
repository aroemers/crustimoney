(ns crustimoney2.string-grammar
  (:require [crustimoney2.core :as core]
            [crustimoney2.combinators :refer :all]))


(def grammar
  (core/rmap
   {:non-terminal (with-name :non-terminal
                    (with-value
                      (regex "[a-zA-Z_]+")))

    :string (chain (literal "'")
                   (with-name :string
                     (with-value
                       (regex "[^']*")))
                   (literal "'"))

    :group (chain (literal "(")
                  (with-name :group
                    (ref :choice))
                  (literal ")"))

    :character-class (with-name :character-class
                       (with-value
                         (regex #"\[[^]]*]")))

    :expr (choice (ref :non-terminal)
                  (ref :group)
                  (ref :string)
                  (ref :character-class))

    :chain (choice (with-name :chain
                     (chain (repeat+ (chain (ref :expr)
                                            (literal " ")))
                            (ref :expr)))
                   (ref :expr))

    :choice (choice (with-name :choice
                      (chain (repeat+ (chain (ref :chain)
                                             (literal "/")))
                             (ref :chain)))
                    (ref :chain))}))

(defn parse [text]
  (core/parse (:choice grammar) text))
