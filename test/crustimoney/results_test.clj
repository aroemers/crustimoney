(ns crustimoney.results-test
  (:require [clojure.set :as set]
            [clojure.test :refer [deftest testing is]]
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

(deftest transform-test
  (testing "not a success"
    (is (= :foo (r/transform :foo "" nil))))

  (testing "no transformer"
    (let [node [:foo {:start 0, :end 3}]]
      (is (= node (r/transform node "" nil)))))

  (testing "only children have transformer"
    (let [node [:foo {:start 0, :end 3}
                [:bar {:start 0, :end 3}]]]
      (is (= [:foo {:start 0, :end 3} "child"]
             (r/transform node "bar" {:bar (constantly "child")})))))

  (testing "postwalk order"
    (let [node [:plus {:start 0, :end 2}
                [:number {:start 0, :end 2}]
                [:number {:start 3, :end 5}]]]
      (is (= 42 (r/transform node "20 22"
                  {:plus   (r/unite +)
                   :number (r/coerce parse-long)}))))))
