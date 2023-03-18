(ns crustimoney.string-grammar-test
  (:require [clojure.test :refer [deftest testing is]]
            [crustimoney.core :as core]
            [crustimoney.results :as r]
            [crustimoney.string-grammar :refer [create-parser]]))

(deftest create-parser-test
  (testing "simple literal"
    (let [p (create-parser "'foo'")]
      (is (= (r/->success 0 3) (core/parse p "foo")))
      (is (= #{{:key :expected-literal :at 0 :detail {:literal "foo"}}}
             (core/parse p "bar")))))

  (testing "escapes in literal"
    (let [p (create-parser "'foo\\'bar'")]
      (is (= (r/->success 0 7) (core/parse p "foo'bar")))
      (is (= #{{:key :expected-literal :at 0 :detail {:literal "foo'bar"}}}
             (core/parse p "foobar")))))

  (testing "simple character-class"
    (let [p (create-parser "[a-zA-Z]+")]
      (is (= (r/->success 0 3) (core/parse p "Foo")))
      (is (= #{{:key :expected-match :at 0 :detail {:regex "[a-zA-Z]+"}}}
             (core/parse p "123")))))

  (testing "escapes in character-class"
    (let [p (create-parser "[a-zA-Z\\]]+")]
      (is (= (r/->success 0 7) (core/parse p "foo]bar")))))

  (testing "regular expression"
    (let [p (create-parser "#'\\d+'")]
      (is (= (r/->success 0 2) (core/parse p "42")))
      (is (= #{(r/->error :expected-match 0 {:regex "\\d+"})}
             (core/parse p "nan")))))

  (testing "end-of-line $"
    (let [p (create-parser "'foo' $")]
      (is (= (r/->success 0 3) (core/parse p "foo")))
      (is (= #{{:key :unexpected-match :at 3 :detail {:text "b"}}}
             (core/parse p "foobar")))))

  (testing "nameless group"
    (let [p (create-parser "('foo' 'bar')+")]
      (is (= (r/->success 0 6) (core/parse p "foobar")))
      (is (= (r/->success 0 12) (core/parse p "foobarfoobar")))))

  (testing "named group"
    (let [p (create-parser "(:foobar 'foo' 'bar')+")]
      (is (= (r/->success 0 12 [(r/with-success-name :foobar (r/->success 0 6))
                                (r/with-success-name :foobar (r/->success 6 12))])
             (core/parse p "foobarfoobar")))))

  (testing "recursive grammars"
    (let [p (create-parser "expr <- foo bar foo <- 'foo'\nbar <- 'bar'")]
      (is (r/success? (core/parse (:expr p) "foobar")))))

  (testing "star quantifier"
    (let [p (create-parser "'foo'*")]
      (is (= (r/->success 0 0) (core/parse p "")))
      (is (= (r/->success 0 3) (core/parse p "foo")))
      (is (= (r/->success 0 6) (core/parse p "foofoo")))
      (is (= (r/->success 0 0) (core/parse p "bar")))))

  (testing "plus quantifier"
    (let [p (create-parser "'foo'+")]
      (is (= (r/->success 0 0) (core/parse p "")))
      (is (= (r/->success 0 3) (core/parse p "foo")))
      (is (= (r/->success 0 6) (core/parse p "foofoo")))
      (is (= (r/->success 0 0) (core/parse p "bar"))))))
