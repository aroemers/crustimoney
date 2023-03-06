(ns crustimoney2.combinators-test
  (:require [clojure.test :refer [deftest testing is]]
            [crustimoney2.combinators :as c]
            [crustimoney2.core :as core]
            [crustimoney2.results :as r]))

;;; Utilities

(defn parse [parser text]
  (core/parse parser text {:keep-nameless? true}))

;;; Primitives

(deftest literal-test
  (let [p (c/literal "foo")]
    (is (= (r/->success 0 3) (parse p "foo")))
    (is (= (r/->success 0 3) (parse p "foobar")))
    (is (= #{(r/->error :expected-literal 0 {:literal "foo"})}
           (core/parse p "barfoo")))))

(deftest chain-test
  (testing "chain with two parsers"
    (let [p (c/chain (c/literal "foo") (c/literal "bar"))]
      (is (= (r/->success 0 6 [(r/->success 0 3) (r/->success 3 6)])
             (parse p "foobar")))
      (is (= (r/->success 0 6 [(r/->success 0 3) (r/->success 3 6)])
             (parse p "foobarbaz")))
      (is (= #{(r/->error :expected-literal 0 {:literal "foo"})}
             (parse p "barfoo")))
      (is (= #{(r/->error :expected-literal 3 {:literal "bar"})}
             (parse p "foobaz")))))

  (testing "chain with no parsers"
    (let [p (c/chain)]
      (is (= (r/->success 0 0) (parse p "anything"))))))

(deftest choice-test
  (testing "choice with two parsers"
    (let [p (c/choice (c/literal "bar") (c/literal "baz"))]
      (is (= (r/->success 0 3 [(r/->success 0 3)])
             (parse p "bar")))
      (is (= (r/->success 0 3 [(r/->success 0 3)])
             (parse p "baz")))
      (is (= (r/->success 0 3 [(r/->success 0 3)])
             (parse p "barz")))
      (is (= #{(r/->error :expected-literal 0 {:literal "bar"})
               (r/->error :expected-literal 0 {:literal "baz"})}
             (parse p "foo")))))

  (testing "choice with no parsers"
    (let [p (c/choice)]
      (is (= (r/->success 0 0) (parse p "anything"))))))

(deftest repeat*-test
  (let [p (c/repeat* (c/literal "foo"))]
    (is (= (r/->success 0 0) (parse p "anything")))
    (is (= (r/->success 0 3 [(r/->success 0 3)])
           (parse p "foo")))
    (is (= (r/->success 0 6 [(r/->success 0 3) (r/->success 3 6)])
           (parse p "foofoobar")))))

(deftest negate-test
  (let [p (c/negate (c/literal "foo"))]
    (is (= (r/->success 0 0) (parse p "notfoo")))
    (is (= #{(r/->error :unexpected-match 0 {:text "foo"})}
           (parse p "foobar")))))

;;; Extra combinators

(deftest regex-test
  (testing "non-empty regex"
    (let [p (c/regex "foobar+")]
      (is (= (r/->success 0 6) (parse p "foobar")))
      (is (= (r/->success 0 8) (parse p "foobarrr")))
      (is (= #{(r/->error :expected-match 0 {:regex "foobar+"})}
             (parse p "contains-foobar")))))

  (testing "empty regex"
    (let [p (c/regex "")]
      (is (= (r/->success 0 0) (parse p "anything"))))))

(deftest repeat+-test
  (let [p (c/repeat+ (c/literal "foo"))]
    (is (= (r/->success 0 3 [(r/->success 0 3)])
           (parse p "foobar")))
    (is (= (r/->success 0 6 [(r/->success 0 3) (r/->success 3 6)])
           (parse p "foofoo")))
    (is (= #{(r/->error :expected-literal 0 {:literal "foo"})}
           (parse p "not-foo")))))

(deftest lookahead-test
  (let [p (c/lookahead (c/literal "foo"))]
    (is (= (r/->success 0 0) (parse p "foo")))
    (is (= #{(r/->error :failed-lookahead 0)}
           (parse p "not-foo")))))

(deftest maybe-test
  (let [p (c/maybe (c/literal "foo"))]
    (is (= (r/->success 0 0) (parse p "not-foo")))
    (is (= (r/->success 0 3) (parse p "foobar")))))

(deftest eof-test
  (testing "standalone eof"
    (is (= (r/->success 0 0) (parse c/eof "")))
    (is (= #{(r/->error :eof-not-reached 0)}
           (parse c/eof "more"))))

  (testing "wrapping other parser"
    (let [p (c/eof (c/literal "foo"))]
      (is (= (r/->success 0 3) (parse p "foo")))
      (is (= #{(r/->error :eof-not-reached 3)}
             (parse p "foo-and-more"))))))

;;; Result wrappers

(deftest with-name-test
  (let [p (c/with-name :foo (c/literal "foo"))]
    (is (= (r/with-success-name :foo (r/->success 0 3))
           (parse p "foo")))
    (is (= #{(r/->error :expected-literal 0 {:literal "foo"})}
           (parse p "not-foo")))))

(deftest with-error-test
  (let [p (c/with-error :fail (c/literal "foo"))]
    (is (= (r/->success 0 3) (parse p "foo")))
    (is (= #{(r/->error :fail 0)} (parse p "not-foo")))))

(deftest with-value-test
  (testing "without transform function"
    (let [p (c/with-value (c/literal "foo"))]
      (is (= "foo" (r/success->attr (parse p "foobar") :value)))
      (is (= #{(r/->error :expected-literal 0 {:literal "foo"})}
             (parse p "not-foo")))))

  (testing "with transform function"
    (let [p (c/with-value parse-long (c/regex "[0-9]+"))]
      (is (= 42 (r/success->attr (parse p "42cm") :value))))))