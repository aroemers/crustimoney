(ns crustimoney.experimental.combinators-test
  (:require [clojure.test :refer [deftest testing is]]
            [crustimoney.combinators :as c]
            [crustimoney.experimental.combinators :as ec]
            [crustimoney.experimental.results :as er]
            [crustimoney.core :as core]
            [crustimoney.results :as r]))

(deftest with-callback-test
  (let [result (atom [])]
    #_{:clj-kondo/ignore [:inline-def]}
    (defn callback [text success]
      (reset! result [text success]))

    (testing "with-callback invocation on success"
      (let [p (ec/with-callback callback (c/with-name :x (c/literal "x")))]
        (is (= (r/with-success-name :x (r/->success 0 1))
               (core/parse p "x")))
        (is (= ["x" (r/with-success-name :x (r/->success 0 1))]
               @result))))

    (testing "resolving with-callback function"
      (reset! result nil)
      (let [p (ec/with-callback `callback (c/with-name :x (c/literal "x")))]
        (core/parse p "x")
        (is (= ["x" (r/with-success-name :x (r/->success 0 1))] @result))))

    (testing "with-callback not called on errors"
      (reset! result nil)
      (let [p (ec/with-callback callback (c/literal "x"))]
        (core/parse p "not-x")
        (is (nil? @result))))))

(deftest stream*-test
  (testing "zero matches"
    (let [p (ec/stream* (c/literal "x"))]
      (is (= (r/->success 0 0) (core/parse p "not-x")))))

  (testing "multiple matches, no children"
    (let [p (ec/stream* (c/with-name :x (c/literal "x")))]
      (is (= (r/->success 0 3) (core/parse p "xxx"))))))

(deftest stream+-test
  (testing "zero matches"
    (let [p (ec/stream+ (c/literal "x"))]
      (is (= #{(r/->error :expected-literal 0 {:literal "x"})}
             (core/parse p "not-x")))))

  (testing "multiple matches, no children"
    (let [p (ec/stream+ (c/with-name :x (c/literal "x")))]
      (is (= (r/->success 0 3) (core/parse p "xxx"))))))

(deftest recover-test
  (let [p (ec/recover (c/literal "x") (c/regex #"y+"))]
    (testing "no recovery needed"
      (is (= (r/->success 0 1) (core/parse p "x"))))

    (testing "recovered"
      (let [result (core/parse p "yyy")]
        (is (= (er/with-success-recovered-errors #{(r/->error :expected-literal 0 {:literal "x"})}
                 (r/with-success-name :crusti/recovered
                   (r/->success 0 3)))
               result))
        (is (= #{(r/->error :expected-literal 0 {:literal "x"})}
               (er/success->recovered-errors result)))))

    (testing "recovery failed"
      (is (= #{(r/->error :expected-literal 0 {:literal "x"})}
             (core/parse p "zzz"))))))

(deftest range-test
  (testing "illegal ranges"
    (is (thrown-with-msg? AssertionError #"min must at least be 0, and max must at least be min"
          (ec/range (c/literal "x") -1 5)))
    (is (thrown-with-msg? AssertionError #"min must at least be 0, and max must at least be min"
          (ec/range (c/literal "x") 5 3))))

  (testing "max range of 0"
    (let [p (ec/range (c/literal "x") 0 0)]
      (is (= (r/->success 0 0) (core/parse p "xxx")))))

  (testing "min range of 0"
    (let [p (ec/range (c/literal "x") 0 2)]
      (is (= (r/->success 0 0) (core/parse p "yyy")))))

  (let [p (ec/range (c/literal "x") 1 3)]
    (testing "not enough matches"
      (is (= #{(r/->error :expected-literal 0 {:literal "x"})}
             (core/parse p "not-x"))))

    (testing "just enough matches"
      (is (= (r/->success 0 1) (core/parse p "xyyy"))))

    (testing "more than min, less than max"
      (is (= (r/->success 0 2) (core/parse p "xxyy"))))

    (testing "max matches"
      (is (= (r/->success 0 3) (core/parse p "xxxy"))))

    (testing "not more than max matches"
      (is (= (r/->success 0 3) (core/parse p "xxxx"))))))
