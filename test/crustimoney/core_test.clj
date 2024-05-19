(ns crustimoney.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [crustimoney.core :as core]
            [crustimoney.combinator-grammar :as c]
            [crustimoney.results :as r]))

(deftest parse-test
  (testing "start at non-zero index"
    (let [p (c/literal "bar")]
      (is (r/success? (core/parse p "foobar" {:index 3})))))

  (testing "without a cache"
    (let [p (c/literal "foo")]
      (is (r/success? (core/parse p "foobar" {:cache nil})))))

  (testing "resiliency against infinite loops"
    (let [grammar {:root (c/chain :b), :b (c/chain :c), :c (c/chain :root)}]
      (is (thrown-with-msg? Exception #"Infinite parsing loop detected"
                            (core/parse grammar "anything"))))

    (let [grammar {:root (c/chain (c/choice (c/maybe (c/repeat* :b))))
                   :b    (c/chain :root)}]
      (is (thrown-with-msg? Exception #"Infinite parsing loop detected"
                            (core/parse grammar "anything")))))

  (testing "resiliency against stack overflow"
    (let [grammar   {:root (c/choice (c/chain (c/literal "a") :root)
                                     (c/literal "a"))}
          long-text (apply str (repeat 10000 "a"))]
      (is (r/success? (core/parse grammar long-text)))))

  (testing "report unknown parser function result"
    (let [thrown (try (core/parse (constantly :whut) "anything") (catch Exception e e))]
      (is (= "Unexpected result from parser" (.getMessage thrown)))
      (is (= clojure.lang.Keyword (:type (ex-data thrown)))))))
