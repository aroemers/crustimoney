(ns crustimoney.combinators.experimental-test
  (:require [clojure.test :refer [deftest is]]
            [crustimoney.combinators :as c]
            [crustimoney.combinators.experimental :as e]
            [crustimoney.core :as core]))

(deftest stream-test
  (is (core/parse (e/stream prn (c/literal "x")) "xxx")))

(deftest recover-test
  (is (core/parse (e/recover (c/literal "x") (c/literal "y")) "y")))

(deftest range-test
  (is (core/parse (e/range (c/literal "x") 2 4) "xxx")))
