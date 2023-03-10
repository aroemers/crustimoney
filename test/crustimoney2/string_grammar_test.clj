(ns crustimoney2.string-grammar-test
  (:require [clojure.test :refer [deftest testing is]]
            [crustimoney2.core :as core]
            [crustimoney2.results :as r]
            [crustimoney2.string-grammar :refer [create-parser]]))

(deftest create-parser-test
  (testing "simple literal"
    (let [p (create-parser "'foo'")]
      (is (= (r/->success 0 3) (core/parse p "foo"))))))
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
)
