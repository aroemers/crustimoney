(ns crustimoney.quick-test
  (:require [clojure.test :refer [deftest testing is]]
            [crustimoney.quick :as quick]))

(deftest parse-test
  (testing "string grammar"
    (is (= [nil "alice and bob and eve" [:who "bob"] [:who "eve"]]
           (quick/parse "'alice' (' and ' (:who word))+"
                        "alice and bob and eve"))))

  (testing "data grammar"
    (is (= [nil "alice and bob and eve" [:who "bob"] [:who "eve"]]
           (quick/parse '("alice" (" and " (:who word))+)
                        "alice and bob and eve"))))

  (testing "no match"
    (is (= nil (quick/parse "'foobar'" "foobaz")))))
