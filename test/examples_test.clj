(ns ^:examples examples-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest testing is]]
            [crustimoney.core :as core]
            [crustimoney.data-grammar :as data-grammar]
            [crustimoney.results :as r]
            [crustimoney.string-grammar :as string-grammar]
            [crustimoney.vector-grammar :as vector-grammar]))

;;; Read input

(defn- from [filename]
  (slurp (io/reader (str "examples/" filename))))

(defn- from-peg [filename]
  (-> (from filename)
      (string-grammar/create-parser)))

(defn- from-clj [filename]
  (-> (from filename)
      (read-string)
      (data-grammar/create-parser)))

(defn- from-edn [filename]
  (->> (from filename)
       (edn/read-string {:readers *data-readers*})
       (data-grammar/create-parser)))

;;; Examples tests

(deftest calc-test
  (let [input    "1+2-(42*8)"
        expected [nil {:start 0, :end 10}
                  [:sum {:start 0, :end 10}
                   [:number {:start 0, :end 1}]
                   [:operation {:start 1, :end 2}]
                   [:sum {:start 2, :end 10}
                    [:number {:start 2, :end 3}]
                    [:operation {:start 3, :end 4}]
                    [:product {:start 5, :end 9}
                     [:number {:start 5, :end 7}]
                     [:operation {:start 7, :end 8}]
                     [:number {:start 8, :end 9}]]]]]]

    (testing "string grammar"
      (let [p (from-peg "calc.peg")]
        (is (= expected (core/parse (:sum p) input)))))

    (testing "data grammar"
      (let [p (from-clj "calc.clj")]
        (is (= expected (core/parse (:sum p) input)))))

    (testing "edn grammar"
      (let [p (from-edn "calc.edn")]
        (is (= expected (core/parse (:sum p) input)))))))

(deftest json-test
  (let [input    "[{\"bool\": true, \"not bool\":false ,\"int\": -83.4,
                    \"nested\" :{} }, null ]"
        expected [:root {:start 0, :end 90}
                  [:array {:start 0, :end 90}
                   [:object {:start 1, :end 82}
                    [:entry {:start 2, :end 14} [:string {:start 3, :end 7}] [:boolean {:start 10, :end 14}]]
                    [:entry {:start 16, :end 32} [:string {:start 17, :end 25}] [:boolean {:start 27, :end 32}]]
                    [:entry {:start 34, :end 46} [:string {:start 35, :end 38}] [:number {:start 41, :end 46}]]
                    [:entry {:start 68, :end 80} [:string {:start 69, :end 75}] [:object {:start 78, :end 80}]]]
                   [:null {:start 84, :end 88}]]]]

    (testing "string grammar"
      (let [p (from-peg "json.peg")]
        (is (= expected (core/parse (:root p) input)))))

    (testing "data grammar"
      (let [p (from-clj "json.clj")]
        (is (= expected (core/parse (:root p) input)))))))

(deftest string-grammar-test
  (let [input (from "string-grammar.peg")]
    (testing "string grammar"
      (let [parser (from-peg "string-grammar.peg")
            result (core/parse (:root parser) input)
            vtree  (string-grammar/vector-tree-for input result)
            parser (vector-grammar/create-parser vtree)
            result (core/parse (:root parser) input)]
        (is (r/success? result))))

    (testing "data grammar"
      (let [parser (from-clj "string-grammar.clj")
            result (core/parse (:root parser) input)
            vtree  (string-grammar/vector-tree-for input result)
            parser (vector-grammar/create-parser vtree)
            result (core/parse (:root parser) input)]
        (is (r/success? result))))))
