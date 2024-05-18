(ns crustimoney.combinator-grammar-test
  (:require [clojure.test :refer [deftest testing is]]
            [crustimoney.combinator-grammar :as cg]))

;;; Primitives

(deftest literal-test
  (testing "correct literal model"
    (is (= [:literal {:text "foo"}] (cg/literal "foo")))))

(deftest chain-test
  (testing "empty chain"
    (is (= [:chain] (cg/chain))))

  (testing "multi chain, transforming refs"
    (is (= [:chain
            [:literal {:text "foo"}]
            [:ref {:to :bar}]
            [:literal {:text "baz"}]]
           (cg/chain (cg/literal "foo") :bar (cg/literal "baz")))))

  (testing "transforming cuts"
    (is (= [:chain [:ref {:to :foo}] :soft-cut :hard-cut]
         (cg/chain :foo cg/soft-cut cg/hard-cut))))

  (testing "empty attribute map on parser map"
    (is (= [:chain {} {:root [:literal {:text "foo"}]}]
           (cg/chain {:root (cg/literal "foo")})))))

(deftest choice-test
  (testing "empty choice"
    (is (= [:choice] (cg/choice))))

  (testing "multi choice, transforming refs"
    (is (= [:choice
            [:ref {:to :foo}]
            [:literal {:text "bar"}]]
           (cg/choice :foo (cg/literal "bar")))))

  (testing "empty attribute map on parser map"
    (is (= [:choice {} {:root [:literal {:text "foo"}]}]
           (cg/choice {:root (cg/literal "foo")})))))

(deftest repeat*-test
  (testing "normal repeat"
    (is (= [:repeat* [:literal {:text "foo"}]] (cg/repeat* (cg/literal "foo")))))

  (testing "referring repeat"
    (is (= [:repeat* [:ref {:to :foo}]] (cg/repeat* :foo))))

  (testing "empty attribute map on parser map"
    (is (= [:repeat* {} {:root [:literal {:text "foo"}]}]
           (cg/repeat* {:root (cg/literal "foo")})))))

(deftest negate-test
  (testing "normal negate"
    (is (= [:negate [:literal {:text "foo"}]] (cg/negate (cg/literal "foo")))))

  (testing "referring negate"
    (is (= [:negate [:ref {:to :foo}]] (cg/negate :foo))))

  (testing "empty attribute map on parser map"
    (is (= [:negate {} {:root [:literal {:text "foo"}]}]
           (cg/negate {:root (cg/literal "foo")})))))

;;; Extra combinators

(deftest regex-test
  (testing "correct regex model"
    (is (= [:regex {:pattern "foo"}] (cg/regex "foo")))))

(deftest repeat+-test
  (testing "normal repeat"
    (is (= [:repeat+ [:literal {:text "foo"}]] (cg/repeat+ (cg/literal "foo")))))

  (testing "referring repeat"
    (is (= [:repeat+ [:ref {:to :foo}]] (cg/repeat+ :foo))))

  (testing "empty attribute map on parser map"
    (is (= [:repeat+ {} {:root [:literal {:text "foo"}]}]
           (cg/repeat+ {:root (cg/literal "foo")})))))

(deftest lookahead-test
  (testing "normal lookahead"
    (is (= [:lookahead [:literal {:text "foo"}]] (cg/lookahead (cg/literal "foo")))))

  (testing "referring lookahead"
    (is (= [:lookahead [:ref {:to :foo}]] (cg/lookahead :foo))))

  (testing "empty attribute map on parser map"
    (is (= [:lookahead {} {:root [:literal {:text "foo"}]}]
           (cg/lookahead {:root (cg/literal "foo")})))))

(deftest maybe-test
  (testing "normal maybe"
    (is (= [:maybe [:literal {:text "foo"}]] (cg/maybe (cg/literal "foo")))))

  (testing "referring maybe"
    (is (= [:maybe [:ref {:to :foo}]] (cg/maybe :foo))))

  (testing "empty attribute map on parser map"
    (is (= [:maybe {} {:root [:literal {:text "foo"}]}]
           (cg/maybe {:root (cg/literal "foo")})))))

(deftest eof-test
  (testing "eof model"
    (is (= [:eof] (cg/eof)))))

;;; Result wrappers

(deftest with-name-test
  (testing "normal with-name"
    (is (= [:with-name {:key :foo} [:literal {:text "foo"}]]
           (cg/with-name :foo (cg/literal "foo")))))

  (testing "referring with-name"
    (is (= [:with-name {:key :foo} [:ref {:to :bar}]]
           (cg/with-name :foo :bar)))))

(deftest with-error-test
  (testing "normal with-error"
    (is (= [:with-error {:key :foo} [:literal {:text "foo"}]]
           (cg/with-error :foo (cg/literal "foo")))))

  (testing "referring with-error"
    (is (= [:with-error {:key :foo} [:ref {:to :bar}]]
           (cg/with-error :foo :bar)))))

;;; Recursive grammar definition

(deftest ref-test
  (testing "ref model"
    (is (= [:ref {:to :foo}] (cg/ref :foo)))))
