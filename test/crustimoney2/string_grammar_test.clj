(ns crustimoney2.string-grammar-test
  (:require [clojure.test :refer [deftest testing is]]
            [crustimoney2.core :as core]
            [crustimoney2.results :as r]
            [crustimoney2.string-grammar :refer [create-parser]]))

(deftest create-parser-test
  (testing "simple literal"
    (let [p (create-parser "'foo'")]
      (is (= (r/->success 0 3) (core/parse p "foo"))))))
