(ns crustimoney.built-in-test
  (:require [clojure.test :refer [deftest is]]
            [crustimoney.built-in :as b]
            [crustimoney.core :as core]
            [crustimoney.results :as r]))

(deftest space-test
  (is (= (r/->success 0 3) (core/parse b/space " \t\n")))
  (is (= #{(r/->error :expected-space 0)}
         (core/parse b/space "not-space"))))

(deftest space?-test
  (is (= (r/->success 0 3) (core/parse b/space? " \t\n")))
  (is (= (r/->success 0 0) (core/parse b/space? "not-space"))))

(deftest blank-test
  (is (= (r/->success 0 2) (core/parse b/blank " \t")))
  (is (= #{(r/->error :expected-blank 0)}
         (core/parse b/blank "\n"))))

(deftest blank?-test
  (is (= (r/->success 0 2) (core/parse b/blank? " \t")))
  (is (= (r/->success 0 0) (core/parse b/blank? "\n"))))

(deftest integer-test
  (is (= (r/->success 0 3) (core/parse b/integer "123")))
  (is (= (r/->success 0 3) (core/parse b/integer "-23")))
  (is (= (r/->success 0 1) (core/parse b/integer "0")))
  (is (= #{(r/->error :expected-integer 0)}
         (core/parse b/integer "nan"))))

(deftest natural-test
  (is (= (r/->success 0 3) (core/parse b/natural "123")))
  (is (= (r/->success 0 1) (core/parse b/natural "0")))
  (is (= #{(r/->error :expected-natural-number 0)}
         (core/parse b/natural "nan")))
  (is (= #{(r/->error :expected-natural-number 0)}
         (core/parse b/natural "-10"))))

(deftest word-test
  (is (= (r/->success 0 3) (core/parse b/word "Foo")))
  (is (= #{(r/->error :expected-word 0)}
         (core/parse b/word "42"))))

(deftest dquote-test
  (is (= (r/->success 0 2) (core/parse b/dquote "\"\"")))
  (is (= (r/->success 0 5) (core/parse b/dquote "\"foo\"")))
  (is (= (r/->success 0 10) (core/parse b/dquote "\"foo\\\"bar\"")))
  (is (= #{(r/->error :expected-double-qoute-string 0)}
         (core/parse b/dquote "\"foo"))))

(deftest squote-test
  (is (= (r/->success 0 2) (core/parse b/squote "''")))
  (is (= (r/->success 0 5) (core/parse b/squote "'foo'")))
  (is (= (r/->success 0 10) (core/parse b/squote "'foo\\'bar'")))
  (is (= #{(r/->error :expected-single-qoute-string 0)}
         (core/parse b/squote "'foo"))))
