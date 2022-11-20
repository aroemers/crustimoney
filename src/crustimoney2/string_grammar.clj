(ns crustimoney2.string-grammar
  (:require [crustimoney2.core :as core]
            [crustimoney2.combinators :refer :all]
            [crustimoney2.results :as r]))

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
  (core/parse (eof (:choice grammar)) text))

(defmulti parser-for
  (fn [node]
    (r/success->name node)))

(defn create [text]
  (let [result (parse text)]
    (if (list? result)
      (throw (ex-info "Failed to parse grammar" {:errors (distinct result)}))
      (let [root-node (first (r/success->children result))]
        (parser-for root-node)))))

(defmethod parser-for :non-terminal
  [node]
  (core/ref (keyword (r/success->attr node :value))))

(defmethod parser-for :string
  [node]
  (literal (r/success->attr node :value)))

(defmethod parser-for :group
  [node]
  (parser-for (first (r/success->children node))))

(defmethod parser-for :character-class
  [node]
  (regex (r/success->attr node :value)))

(defmethod parser-for :chain
  [node]
  (apply chain (map parser-for (r/success->children node))))

(defmethod parser-for :choice
  [node]
  (apply choice (map parser-for (r/success->children node))))
