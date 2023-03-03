(ns crustimoney2.vector-grammar-test
  (:require [clojure.test :refer [deftest is testing]]
            [crustimoney2.combinators :as c]
            [crustimoney2.core :as core]
            [crustimoney2.results :as r]
            [crustimoney2.vector-grammar :refer [create-parser]]))

(deftest create-parser-test
  (testing "simple literal vector"
    (let [p (create-parser [:literal "foo"])]
      (is (= (r/->success 0 3) (core/parse p "foo")))))

  (testing "nested vectors"
    (let [p (create-parser [:chain [:literal "foo"] [:regex "ba(r|z)"]])]
      (is (= (r/->success 0 6) (core/parse p "foobaz")))))

  (testing "map of vectors with refs"
    (let [p (create-parser {:root [:chain [:literal "foo"] [:ref :bax]]
                            :bax  [:regex "ba(r|z)"]})]
      (is (= (r/->success 0 6) (core/parse (:root p) "foobaz")))))

  (testing "arbitrary values"
    (let [p (create-parser {:root [:chain [:literal "foo"] [:with-name :bax [:ref :bax]]]
                            :bax  (c/regex "ba(r|z)")})]
      (is (= (r/->success 0 6 [(r/with-success-name :bax (r/->success 3 6))])
             (core/parse (:root p) "foobaz")))))

  (testing "custom combinator"
    (def my-combinator c/literal)

    (let [p (create-parser [::my-combinator "foo"])]
      (is (= (r/->success 0 3) (core/parse p "foo"))))))
