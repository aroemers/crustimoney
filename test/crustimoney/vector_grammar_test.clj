(ns crustimoney.vector-grammar-test
  (:require [clojure.test :refer [deftest is testing]]
            [crustimoney.combinators :as c]
            [crustimoney.core :as core]
            [crustimoney.results :as r]
            [crustimoney.vector-grammar :as vg]))

(deftest create-parser-test
  (testing "simple literal vector"
    (let [p (vg/compile [:literal {:text "foo"}])]
      (is (= (r/->success 0 3) (core/parse p "foo")))))

  (testing "nested vectors"
    (let [p (vg/compile [:chain [:literal {:text "foo"}] [:regex {:pattern "ba(r|z)"}]])]
      (is (= (r/->success 0 6) (core/parse p "foobaz")))))

  (testing "map of vectors with refs"
    (let [p (vg/compile {:root [:chain [:literal {:text "foo"}] [:ref {:to :bax}]]
                         :bax  [:regex {:pattern "ba(r|z)"}]})]
      (is (= (r/->success 0 6) (core/parse p "foobaz")))))

  (testing "map of vectors with = postfix keys"
    (let [p (vg/compile {:root [:chain [:literal {:text "foo"}] [:ref {:to :bax}]]
                         :bax= [:regex {:pattern "ba(r|z)"}]})]
      (is (= (r/->success 0 6 [(r/with-success-name :bax (r/->success 3 6))])
             (core/parse p "foobaz")))))

  (testing "arbitrary values"
    (let [p (vg/compile {:root [:chain
                                [:literal {:text "foo"}]
                                [:with-name {:key :bax}
                                 [:ref {:to :bax}]]]
                         :bax  (c/regex {:pattern "ba(r|z)"})})]
      (is (= (r/->success 0 6 [(r/with-success-name :bax (r/->success 3 6))])
             (core/parse p "foobaz")))))

  (testing "custom combinator"
    #_{:clj-kondo/ignore [:inline-def]}
    (def my-combinator c/literal)

    (let [p (vg/compile [::my-combinator {:text "foo"}])]
      (is (= (r/->success 0 3) (core/parse p "foo")))))

  (testing "missing custom combinator"
    (is (thrown-with-msg? Exception #"Could not resolve combinator key :missing"
          (vg/compile [:missing])))))
