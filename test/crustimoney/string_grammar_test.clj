(ns crustimoney.string-grammar-test
  (:require [clojure.test :refer [deftest testing is]]
            [crustimoney.combinators :as c]
            [crustimoney.core :as core]
            [crustimoney.results :as r]
            [crustimoney.string-grammar :refer [create-parser vector-tree]]))

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

  (testing "chain"
    (let [p (create-parser "'foo' 'bar'")]
      (is (= (r/->success 0 6) (core/parse p "foobar")))
      (is (= #{{:key :expected-literal :at 3, :detail {:literal "bar"}}}
             (core/parse p "foobaz")))))

  (testing "choice"
    (let [p (create-parser "'foo' / 'bar'")]
      (is (= (r/->success 0 3) (core/parse p "foo")))
      (is (= (r/->success 0 3) (core/parse p "bar")))
      (is (= #{{:key :expected-literal :at 0, :detail {:literal "foo"}}
               {:key :expected-literal :at 0, :detail {:literal "bar"}}}
             (core/parse p "neither")))))

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

  (testing "auto-named rule"
    (let [p (create-parser "expr= <- 'foo' $")]
      (is (= (r/with-success-name :expr (r/->success 0 3))
             (core/parse (:expr p) "foo")))))

  (testing "star quantifier"
    (let [p (create-parser "'foo'*")]
      (is (= (r/->success 0 0) (core/parse p "")))
      (is (= (r/->success 0 3) (core/parse p "foo")))
      (is (= (r/->success 0 6) (core/parse p "foofoo")))
      (is (= (r/->success 0 0) (core/parse p "bar")))))

  (testing "plus quantifier"
    (let [p (create-parser "'foo'+")]
      (is (= #{{:key :expected-literal :at 0, :detail {:literal "foo"}}}
             (core/parse p "")))
      (is (= (r/->success 0 3) (core/parse p "foo")))
      (is (= (r/->success 0 6) (core/parse p "foofoo")))))

  (testing "question mark quantifier"
    (let [p (create-parser "'foo'?")]
      (is (= (r/->success 0 0) (core/parse p "")))
      (is (= (r/->success 0 3) (core/parse p "foo")))
      (is (= (r/->success 0 3) (core/parse p "foofoo")))
      (is (= (r/->success 0 0) (core/parse p "bar")))))

  (testing "lookahead"
    (let [p (create-parser "&'foo'")]
      (is (= (r/->success 0 0) (core/parse p "foo")))
      (is (= #{{:key :expected-literal :at 0, :detail {:literal "foo"}}}
             (core/parse p "bar")))))

  (testing "negative lookahead"
    (let [p (create-parser "!'foo'")]
      (is (= (r/->success 0 0) (core/parse p "bar")))
      (is (= #{{:key :unexpected-match :at 0, :detail {:text "foo"}}}
             (core/parse p "foo")))))

  (testing "hard cut"
    (let [p (create-parser "('foo' >>)* $ / 'foobar'")]
      (is (= (r/->success 0 6) (core/parse p "foofoo")))
      (is (= #{{:key :unexpected-match, :at 3, :detail {:text "b"}}}
             (core/parse p "foobar")))))

  (testing "soft cut"
    (let [p (create-parser "('foo' > 'bar') 'baz' / 'foobarz'")]
      (is (= (r/->success 0 7) (core/parse p "foobarz")))
      (is (= #{{:key :expected-literal, :at 3, :detail {:literal "bar"}}}
             (core/parse p "foobaz")))))

  (testing "extra rules"
    (let [p (c/grammar (create-parser "root <- foo")
                       {:foo (create-parser "'foo'")})]
      (is (r/success? (core/parse (:root p) "foo")))))

  (testing "report grammar errors"
    (let [thrown (try (create-parser "(foo") (catch Exception e e))]
      (is (= "Failed to parse grammar" (.getMessage thrown)))
      (is (= {:errors #{{:key :expected-literal :at 4
                         :detail {:literal ")"}
                         :line 1 :column 5}}}
             (ex-data thrown))))))

(deftest vector-tree-test
  (is (= [:choice [:chain [:literal "foo"] [:regex "ba(r|z)"]] [:literal "eve"]]
         (vector-tree "'foo' #'ba(r|z)' / 'eve'"))))
