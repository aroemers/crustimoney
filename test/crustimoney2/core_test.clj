(ns crustimoney2.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [crustimoney2.core :as core]
            [crustimoney2.combinators :as c]
            [crustimoney2.results :as r]))

(deftest parse-test
  (testing "start at non-zero index"
    (let [p (c/literal "bar")]
      (is (r/success? (core/parse p "foobar" {:index 3})))))

  (testing "without a cache"
    (let [p (c/literal "foo")]
      (is (r/success? (core/parse p "foobar" {:cache nil})))))

  (testing "keeping nameless nodes"
    (let [p (c/chain (c/literal "foo") (c/literal "bar"))]
      (is (= [nil {:start 0 :end 6}]
             (core/parse p "foobar" {:keep-nameless? false})))
      (is (= [nil {:start 0 :end 6}
              [nil {:start 0 :end 3}]
              [nil {:start 3 :end 6}]]
             (core/parse p "foobar" {:keep-nameless? true})))))

  (testing "resiliency against infinite loops"
    (let [grammar (c/grammar {:a (c/ref :b), :b (c/ref :c), :c (c/ref :a)})]
      (is (thrown-with-msg? Exception #"Infinite parsing loop detected"
                            (core/parse (:a grammar) "anything"))))

    (let [grammar (c/grammar {:a (c/chain (c/choice (c/maybe (c/repeat* (c/ref :b)))))
                              :b (c/ref :a)})]
      (is (thrown-with-msg? Exception #"Infinite parsing loop detected"
                            (core/parse (:a grammar) "anything")))))

  (testing "resiliency against stack overflow"
    (let [grammar   (c/grammar {:a (c/choice (c/chain (c/literal "a") (c/ref :a))
                                             (c/literal "a"))})
          long-text (apply str (repeat 10000 "a"))]
      (is (r/success? (core/parse (:a grammar) long-text))))))
