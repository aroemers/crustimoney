(ns crustimoney.vector-grammar-test
  (:require [clojure.test :refer [deftest is testing]]
            [crustimoney.combinators :as c]
            [crustimoney.core :as core]
            [crustimoney.results :as r]
            [crustimoney.vector-grammar :refer [create-parser]]))

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
      (is (= (r/->success 0 6) (core/parse p "foobaz")))))

  (testing "map of vectors with = postfix keys"
    (let [p (create-parser {:root [:chain [:literal "foo"] [:ref :bax]]
                            :bax= [:regex "ba(r|z)"]})]
      (is (= (r/->success 0 6 [(r/with-success-name :bax (r/->success 3 6))])
             (core/parse p "foobaz")))))

  (testing "arbitrary values"
    (let [p (create-parser {:root [:chain [:literal "foo"] [:with-name :bax [:ref :bax]]]
                            :bax  (c/regex "ba(r|z)")})]
      (is (= (r/->success 0 6 [(r/with-success-name :bax (r/->success 3 6))])
             (core/parse p "foobaz")))))

  (testing "custom combinator"
    #_{:clj-kondo/ignore [:inline-def]}
    (def my-combinator c/literal)

    (let [p (create-parser [::my-combinator "foo"])]
      (is (= (r/->success 0 3) (core/parse p "foo")))))

  (testing "missing custom combinator"
    (is (thrown-with-msg? Exception #"combinator-key does not resolve"
          (create-parser [:missing])))))
