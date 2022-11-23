(ns crustimoney2.string-grammar
  (:refer-clojure :exclude [ref])
  (:require [crustimoney2.core :as core :refer [ref]]
            [crustimoney2.combinators :refer :all]
            [crustimoney2.results :as r]))

;;; Grammar definition

(def grammar
  (core/rmap
   {:non-terminal (with-name :non-terminal
                    (with-value
                      (regex "[a-zA-Z_-]+")))

    :literal (chain (literal "'")
                    (with-name :literal
                      (with-value
                        (regex "[^']*")))
                    (literal "'"))

    :group-name (chain (literal ":")
                       (with-name :group-name
                         (with-value
                           (regex "[a-zA-Z_-]+"))))

    :group (with-name :group
             (chain (literal "(")
                    (maybe (chain (ref :group-name)
                                  (literal " ")))
                    (ref :choice)
                    (literal ")")))

    :character-class (with-name :character-class
                       (with-value
                         (regex #"\[[^]]*]")))

    :expr (choice (ref :non-terminal)
                  (ref :group)
                  (ref :literal)
                  (ref :character-class))

    :quantified (choice (with-name :quantified
                          (chain (ref :expr)
                                 (with-name :operand
                                   (with-value
                                     (regex "[?+*]")))))
                        (ref :expr))

    :lookahead (choice (with-name :lookahead
                         (chain (with-name :operand
                                  (with-value
                                    (regex "[&!]")))
                                (ref :quantified)))
                       (ref :quantified))

    :chain (choice (with-name :chain
                     (chain (repeat+ (chain (ref :lookahead)
                                            (literal " ")))
                            (ref :lookahead)))
                   (ref :lookahead))

    :choice (choice (with-name :choice
                      (chain (repeat+ (chain (ref :chain)
                                             (literal "/")))
                             (ref :chain)))
                    (ref :chain))

    :root (eof (ref :choice))}))


;;; Parse result processing

(defmulti parser-for
  (fn [node]
    (r/success->name node)))

(defn create [text]
  (let [result (core/parse (:root grammar) text)]
    (if (list? result)
      (throw (ex-info "Failed to parse grammar" {:errors (distinct result)}))
      (let [root-node (first (r/success->children result))]
        (core/rmap {:root (parser-for root-node)})))))

(defmethod parser-for :non-terminal
  [node]
  (core/ref (keyword (r/success->attr node :value))))

(defmethod parser-for :literal
  [node]
  (literal (r/success->attr node :value)))

(defmethod parser-for :group
  [node]
  (let [[child1 child2] (r/success->children node)]
    (if (= (r/success->name child1) :group-name)
      (with-name (keyword (r/success->attr child1 :value))
        (parser-for child2))
      (parser-for child1))))

(defmethod parser-for :character-class
  [node]
  (regex (r/success->attr node :value)))

(defmethod parser-for :chain
  [node]
  (apply chain (map parser-for (r/success->children node))))

(defmethod parser-for :choice
  [node]
  (apply choice (map parser-for (r/success->children node))))

(defmethod parser-for :lookahead
  [node]
  (let [[operand expr] (r/success->children node)
        parser         (parser-for expr)]
    (case (r/success->attr operand :value)
      "!" (negate parser)
      "&" (lookahead parser))))

(defmethod parser-for :quantified
  [node]
  (let [[expr operand] (r/success->children node)
        parser         (parser-for expr)]
    (case (r/success->attr operand :value)
      "?" (maybe parser)
      "+" (repeat+ parser)
      "*" (repeat* parser))))
