(ns crustimoney.experimental.reader-test
  (:require [clojure.test :refer [deftest testing is]]
            [crustimoney.combinators :as c]
            [crustimoney.core :as core]
            [crustimoney.experimental.reader :as reader]
            [crustimoney.results :as r])
  (:import [java.io StringReader]))

(deftest match-literal-test
  (testing "on a reader"
    (let [r (reader/wrap-reader (StringReader. "foobar") 2)]
      (is (= (r/->success 0 3) (reader/match-literal r 0 "foo")))
      (is (nil? (reader/match-literal r 0 "foobarbaz")))))

  (testing "on a string"
    (is (= (r/->success 0 3) (reader/match-literal "foobar" 0 "foo"))))

  (testing "str on a reader"
    (let [r (reader/wrap-reader (StringReader. "foobar") 2)]
      (.charAt r 2)
      (is (= "foob" (str r))))))

(deftest match-pattern-test
  (testing "on a reader"
    (let [r (reader/wrap-reader (StringReader. "foobar") 2)]
      (is (= (r/->success 0 3) (reader/match-pattern r 0 #"fo.")))
      (is (nil? (reader/match-pattern r 0 #"foobarbaz")))))

  (testing "on a string"
    (is (= (r/->success 0 3) (reader/match-pattern "foobar" 0 #"foo")))))

(deftest cut-test
  (testing "directly on reader"
    (let [r (reader/wrap-reader (StringReader. "foobar") 2)]
      (is (= \b (.charAt r 3)))
      (reader/cut r 3)
      (is (= \b (.charAt r 3)))
      (is (thrown? StringIndexOutOfBoundsException
             (.charAt r 2)))))

  (testing "via hard-cut during parsing"
    (let [p (c/chain (c/literal "foo") :hard-cut)
          r (reader/wrap-reader (StringReader. "foobar") 2)]
      (is (= (r/->success 0 3) (core/parse p r)))
      (is (= "bar" (.subSequence r 3 6)))
      (is (thrown? StringIndexOutOfBoundsException
            (.subSequence r 0 3))))))

(deftest reader?-test
  (is (reader/reader? (StringReader. "foobar")))
  (is (not (reader/reader? "foobar"))))
