(ns crustimoney.data-grammar-test
  (:require [clojure.test :refer [deftest testing is]]
            [crustimoney.combinators :as c]
            [crustimoney.core :as core]
            [crustimoney.results :as r]
            [crustimoney.data-grammar :refer [create-parser]]))

(deftest create-parser-test
  (testing "creating a vector model"
    (is (= [:choice [:chain
                     [:literal {:text "foo"}]
                     [:literal {:text "bar"}]]
            [:literal {:text "eve"}]]
           (create-parser '("foo" "bar" / "eve")))))

  (testing "simple literal"
    (let [p (create-parser "foo")]
      (is (= (r/->success 0 3) (core/parse p "foo")))
      (is (= #{{:key :expected-literal :at 0 :detail {:literal "foo"}}}
             (core/parse p "bar")))))

  (testing "character"
    (let [p (create-parser \f)]
      (is (= (r/->success 0 1) (core/parse p "foo")))
      (is (= #{{:key :expected-literal :at 0 :detail {:literal "f"}}}
             (core/parse p "bar")))))

  (testing "regular expression"
    (let [r #"\d+"
          p (create-parser r)]
      (is (= (r/->success 0 2) (core/parse p "42")))
      (is (= #{(r/->error :expected-match 0 {:regex r})}
             (core/parse p "nan")))))

  (testing "end-of-line $"
    (let [p (create-parser '("foo" $))]
      (is (= (r/->success 0 3) (core/parse p "foo")))
      (is (= #{{:key :unexpected-match :at 3 :detail {:text "b"}}}
             (core/parse p "foobar")))))

  (testing "chain"
    (let [p (create-parser '("foo" "bar"))]
      (is (= (r/->success 0 6) (core/parse p "foobar")))
      (is (= #{{:key :expected-literal :at 3, :detail {:literal "bar"}}}
             (core/parse p "foobaz")))))

  (testing "choice"
    (let [p (create-parser '("foo" / "bar"))]
      (is (= (r/->success 0 3) (core/parse p "foo")))
      (is (= (r/->success 0 3) (core/parse p "bar")))
      (is (= #{{:key :expected-literal :at 0, :detail {:literal "foo"}}
               {:key :expected-literal :at 0, :detail {:literal "bar"}}}
             (core/parse p "neither")))))

  (testing "named group"
    (let [p (create-parser '((:foobar "foo" "bar")+))]
      (is (= (r/->success 0 12 [(r/with-success-name :foobar (r/->success 0 6))
                                (r/with-success-name :foobar (r/->success 6 12))])
             (core/parse p "foobarfoobar")))))

  (testing "recursive grammars"
    (let [p (create-parser '{root (foo bar), foo "foo", bar "bar"})]
      (is (r/success? (core/parse p "foobar")))))

  (testing "auto-named rule"
    (let [p (create-parser '{root= ("foo" $)})]
      (is (= (r/with-success-name :root (r/->success 0 3))
             (core/parse p "foo")))))

  (testing "star quantifier"
    (let [p (create-parser '("foo"*))]
      (is (= (r/->success 0 0) (core/parse p "")))
      (is (= (r/->success 0 3) (core/parse p "foo")))
      (is (= (r/->success 0 6) (core/parse p "foofoo")))
      (is (= (r/->success 0 0) (core/parse p "bar")))))

  (testing "plus quantifier"
    (let [p (create-parser '("foo"+))]
      (is (= #{{:key :expected-literal :at 0, :detail {:literal "foo"}}}
             (core/parse p "")))
      (is (= (r/->success 0 3) (core/parse p "foo")))
      (is (= (r/->success 0 6) (core/parse p "foofoo")))))

  (testing "question mark quantifier"
    (let [p (create-parser '("foo"?))]
      (is (= (r/->success 0 0) (core/parse p "")))
      (is (= (r/->success 0 3) (core/parse p "foo")))
      (is (= (r/->success 0 3) (core/parse p "foofoo")))
      (is (= (r/->success 0 0) (core/parse p "bar")))))

  (testing "lookahead"
    (let [p (create-parser '(&"foo"))]
      (is (= (r/->success 0 0) (core/parse p "foo")))
      (is (= #{{:key :expected-literal :at 0, :detail {:literal "foo"}}}
             (core/parse p "bar")))))

  (testing "negative lookahead"
    (let [p (create-parser '(!"foo"))]
      (is (= (r/->success 0 0) (core/parse p "bar")))
      (is (= #{{:key :unexpected-match :at 0, :detail {:text "foo"}}}
             (core/parse p "foo")))))

  (testing "hard cut"
    (let [p (create-parser '(("foo" >>)* $ / "foobar"))]
      (is (= (r/->success 0 6) (core/parse p "foofoo")))
      (is (= #{{:key :unexpected-match, :at 3, :detail {:text "b"}}}
             (core/parse p "foobar")))))

  (testing "soft cut"
    (let [p (create-parser '(("foo" > "bar") "baz" / "foobarz"))]
      (is (= (r/->success 0 7) (core/parse p "foobarz")))
      (is (= #{{:key :expected-literal, :at 3, :detail {:literal "bar"}}}
             (core/parse p "foobaz")))))

  (testing "extra rules"
    (let [p (merge (create-parser '{root foo})
                   {:foo (create-parser "foo")})]
      (is (r/success? (core/parse p "foo")))))

  (testing "unknown type"
    (is (thrown-with-msg? Exception #"Unknown data type"
          (create-parser (range))))))
