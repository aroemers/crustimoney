(ns crustimoney.combinators-test
  (:require [clojure.test :refer [deftest testing is]]
            [crustimoney.combinators :as c]
            [crustimoney.core :as core]
            [crustimoney.results :as r]))

;;; Primitives

(deftest literal-test
  (let [p (c/literal {:text "foo"})]
    (is (= (r/->success 0 3) (core/parse p "foo")))
    (is (= (r/->success 0 3) (core/parse p "foobar")))
    (is (= #{(r/->error :expected-literal 0 {:literal "foo"})}
           (core/parse p "barfoo")))))

(deftest chain-test
  (testing "chain with two parsers"
    (let [p (c/chain (c/literal {:text "foo"}) (c/literal {:text "bar"}))]
      (is (= (r/->success 0 6 [(r/->success 0 3) (r/->success 3 6)])
             (core/parse p "foobar")))
      (is (= (r/->success 0 6 [(r/->success 0 3) (r/->success 3 6)])
             (core/parse p "foobarbaz")))
      (is (= #{(r/->error :expected-literal 0 {:literal "foo"})}
             (core/parse p "barfoo")))
      (is (= #{(r/->error :expected-literal 3 {:literal "bar"})}
             (core/parse p "foobaz")))))

  (testing "chain with no parsers"
    (let [p (c/chain)]
      (is (= (r/->success 0 0) (core/parse p "anything")))))

  (testing "chain with soft-cut"
    (is (thrown? AssertionError (c/chain :soft-cut)))

    (let [p (c/choice (c/chain (c/maybe (c/chain (c/literal {:text "{"})
                                                 :soft-cut
                                                 (c/literal {:text "foo"})
                                                 (c/literal {:text "}"})))
                               (c/literal {:text "bar"}))
                      (c/literal {:text "baz"}))]
      (is (= #{(r/->error :expected-literal 4 {:literal "}"})}
             (core/parse p "{foo")))
      (is (= #{(r/->error :expected-literal 5 {:literal "bar"})
               (r/->error :expected-literal 0 {:literal "baz"})}
             (core/parse p "{foo}eve")))
      (is (= (r/->success 0 8) (core/parse p "{foo}bar")))))

  (testing "chain with hard-cut"
    (is (thrown? AssertionError (c/chain :hard-cut)))

    (let [p (c/choice (c/chain (c/maybe (c/chain (c/literal {:text "{"})
                                                 :hard-cut
                                                 (c/literal {:text "foo"})
                                                 (c/literal {:text "}"})))
                               (c/literal {:text "bar"}))
                      (c/literal {:text "baz"}))]
      (is (= #{(r/->error :expected-literal 4 {:literal "}"})}
             (core/parse p "{foo")))
      (is (= #{(r/->error :expected-literal 5 {:literal "bar"})}
             (core/parse p "{foo}eve")))
      (is (= (r/->success 0 8) (core/parse p "{foo}bar")))))

  (testing "chain with cuts results in correct children"
    (let [p (c/chain (c/literal {:text "foo"}) :soft-cut (c/literal {:text "bar"}))]
      (is (= (r/->success 0 6 [(r/->success 0 3) (r/->success 3 6)])
             (core/parse p "foobar")))))

  (testing "chain with unknown keyword"
    (is (thrown? AssertionError (c/chain (c/literal {:text "foo"}) :unknown)))))

(deftest choice-test
  (testing "choice with two parsers"
    (let [p (c/choice (c/literal {:text "bar"}) (c/literal {:text "baz"}))]
      (is (= (r/->success 0 3 [(r/->success 0 3)])
             (core/parse p "bar")))
      (is (= (r/->success 0 3 [(r/->success 0 3)])
             (core/parse p "baz")))
      (is (= (r/->success 0 3 [(r/->success 0 3)])
             (core/parse p "barz")))
      (is (= #{(r/->error :expected-literal 0 {:literal "bar"})
               (r/->error :expected-literal 0 {:literal "baz"})}
             (core/parse p "foo")))))

  (testing "choice with no parsers"
    (let [p (c/choice)]
      (is (= (r/->success 0 0) (core/parse p "anything"))))))

(deftest repeat*-test
  (let [p (c/repeat* (c/literal {:text "foo"}))]
    (is (= (r/->success 0 0) (core/parse p "anything")))
    (is (= (r/->success 0 3 [(r/->success 0 3)])
           (core/parse p "foo")))
    (is (= (r/->success 0 6 [(r/->success 0 3) (r/->success 3 6)])
           (core/parse p "foofoobar")))))

(deftest negate-test
  (let [p (c/negate (c/literal {:text "foo"}))]
    (is (= (r/->success 0 0) (core/parse p "notfoo")))
    (is (= #{(r/->error :unexpected-match 0 {:text "foo"})}
           (core/parse p "foobar")))))

;;; Extra combinators

(deftest regex-test
  (testing "non-empty regex"
    (let [p (c/regex {:pattern "foobar+"})]
      (is (= (r/->success 0 6) (core/parse p "foobar")))
      (is (= (r/->success 0 8) (core/parse p "foobarrr")))
      (let [result (core/parse p "contains-foobar")
            error  (first result)]
        (is (some? error))
        (is (= :expected-match (r/error->key error)))
        (is (= 0 (r/error->index error)))
        (is (instance? java.util.regex.Pattern (:regex (r/error->detail error)))))))

  (testing "empty regex"
    (let [p (c/regex {:pattern ""})]
      (is (= (r/->success 0 0) (core/parse p "anything"))))))

(deftest repeat+-test
  (let [p (c/repeat+ (c/literal {:text "foo"}))]
    (is (= (r/->success 0 3 [(r/->success 0 3)])
           (core/parse p "foobar")))
    (is (= (r/->success 0 6 [(r/->success 0 3) (r/->success 3 6)])
           (core/parse p "foofoo")))
    (is (= #{(r/->error :expected-literal 0 {:literal "foo"})}
           (core/parse p "not-foo")))))

(deftest lookahead-test
  (let [p (c/lookahead (c/literal {:text "foo"}))]
    (is (= (r/->success 0 0) (core/parse p "foo")))
    (is (= #{(r/->error :expected-literal 0 {:literal "foo"})}
           (core/parse p "not-foo")))))

(deftest maybe-test
  (let [p (c/maybe (c/literal {:text "foo"}))]
    (is (= (r/->success 0 0) (core/parse p "not-foo")))
    (is (= (r/->success 0 3) (core/parse p "foobar")))))

(deftest eof-test
  (let [p (c/eof)]
    (is (= (r/->success 0 0) (core/parse p "")))
    (is (= #{(r/->error :unexpected-match 0 {:text "m"})}
           (core/parse p "more")))))

;;; Result wrappers

(deftest with-name-test
  (let [p (c/with-name {:key :foo} (c/literal {:text "foo"}))]
    (is (= (r/with-success-name :foo (r/->success 0 3))
           (core/parse p "foo")))
    (is (= #{(r/->error :expected-literal 0 {:literal "foo"})}
           (core/parse p "not-foo")))))

(deftest with-error-test
  (let [p (c/with-error {:key :fail} (c/literal {:text "foo"}))]
    (is (= (r/->success 0 3) (core/parse p "foo")))
    (is (= #{(r/->error :fail 0)} (core/parse p "not-foo")))))

;;; Recursive grammar

(deftest grammar-test
  (testing "simple grammar"
    (let [p {:root (c/ref {:to :foo})
             :foo  (c/literal {:text "foo"})}]
      (is (= (r/->success 0 3) (core/parse p "foo")))))

  (testing "auto-capture rules"
    (let [p {:root (c/ref {:to :foo})
             :foo= (c/literal {:text "foo"})}]
      (is (= (r/with-success-name :foo (r/->success 0 3))
             (core/parse p "foo")))))

  (testing "missing references"
    (let [thrown (try (core/compile {:root (c/ref {:to :foo})}) (catch Exception e e))]
      (is (= "Detected unknown keys in refs" (.getMessage thrown)))
      (is (= {:unknown-keys [:foo]} (ex-data thrown))))))
