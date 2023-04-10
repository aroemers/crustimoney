(ns crustimoney.results-test
  (:require [clojure.set :as set]
            [clojure.test :refer [deftest is]]
            [crustimoney.results :as r]))

(deftest errors->line-column-test
  (let [text     "foo\n\nbar"
        expected #{{:at 0 :line 1 :column 1}
                   {:at 2 :line 1 :column 3}
                   {:at 3 :line 1 :column 4}
                   {:at 4 :line 2 :column 1}
                   {:at 5 :line 3 :column 1}
                   {:at 8 :line 3 :column 4}}]
    (is (= expected (r/errors->line-column (set/project expected [:at]) text)))))
