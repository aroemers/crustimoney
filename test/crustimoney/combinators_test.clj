(ns crustimoney.combinators-test
  (:require [clojure.test :refer [deftest testing is]]
            [crustimoney.combinators :as c]
            [crustimoney.core :as core]
            [crustimoney.results :as r]))

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
      (is (= (r/->success 0 0) (parse p "anything")))))

  (testing "chain with soft-cut"
    (is (thrown? AssertionError (c/chain :soft-cut)))

    (let [p (c/choice (c/chain (c/maybe (c/chain (c/literal "{")
                                                 :soft-cut
                                                 (c/literal "foo")
                                                 (c/literal "}")))
                               (c/literal "bar"))
                      (c/literal "baz"))]
      (is (= #{(r/->error :expected-literal 4 {:literal "}"})}
             (core/parse p "{foo")))
      (is (= #{(r/->error :expected-literal 5 {:literal "bar"})
               (r/->error :expected-literal 0 {:literal "baz"})}
             (core/parse p "{foo}eve")))
      (is (= (r/->success 0 8) (core/parse p "{foo}bar")))))

  (testing "chain with hard-cut"
    (is (thrown? AssertionError (c/chain :hard-cut)))

    (let [p (c/choice (c/chain (c/maybe (c/chain (c/literal "{")
                                                 :hard-cut
                                                 (c/literal "foo")
                                                 (c/literal "}")))
                               (c/literal "bar"))
                      (c/literal "baz"))]
      (is (= #{(r/->error :expected-literal 4 {:literal "}"})}
             (core/parse p "{foo")))
      (is (= #{(r/->error :expected-literal 5 {:literal "bar"})}
             (core/parse p "{foo}eve")))
      (is (= (r/->success 0 8) (core/parse p "{foo}bar")))))

  (testing "chain with cuts results in correct children"
    (let [p (c/chain (c/literal "foo") :soft-cut (c/literal "bar"))]
      (is (= (r/->success 0 6 [(r/->success 0 3) (r/->success 3 6)])
             (parse p "foobar")))))

  (testing "chain with unknown keyword"
    (is (thrown? AssertionError (c/chain (c/literal "foo") :unknown)))))

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
      (let [result (parse p "contains-foobar")
            error  (first result)]
        (is (some? error))
        (is (= :expected-match (r/error->key error)))
        (is (= 0 (r/error->index error)))
        (is (instance? java.util.regex.Pattern (:regex (r/error->detail error)))))))

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
    (is (= #{(r/->error :expected-literal 0 {:literal "foo"})}
           (parse p "not-foo")))))

(deftest maybe-test
  (let [p (c/maybe (c/literal "foo"))]
    (is (= (r/->success 0 0) (parse p "not-foo")))
    (is (= (r/->success 0 3) (parse p "foobar")))))

(deftest eof-test
  (let [p (c/eof)]
    (is (= (r/->success 0 0) (parse p "")))
    (is (= #{(r/->error :unexpected-match 0 {:text "m"})}
           (parse p "more")))))

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

;;; Recursive grammar

(deftest grammar-test
  (testing "simple grammar"
    (let [p (c/grammar {:root (c/ref :foo)
                        :foo  (c/literal "foo")})]
      (is (= (r/->success 0 3) (parse (:root p) "foo")))))

  (testing "auto-capture rules"
    (let [p (c/grammar {:root (c/ref :foo)
                        :foo= (c/literal "foo")})]
      (is (= (r/with-success-name :foo (r/->success 0 3))
             (parse (:root p) "foo")))))

  (testing "missing references"
    (let [thrown (try (c/grammar {:root (c/ref :foo)}) (catch Exception e e))]
      (is (= "Detected unknown keys in refs" (.getMessage thrown)))
      (is (= {:unknown-keys [:foo]} (ex-data thrown))))))
