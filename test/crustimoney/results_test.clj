(ns crustimoney.results-test
  (:require [clojure.set :as set]
            [clojure.test :refer [deftest is]]
            [crustimoney.results :as r]))

(deftest error->xxx-test
  (let [e (r/->error :fail 42 {:details "here"})]
    (is (= :fail (r/error->key e)))
    (is (= 42 (r/error->index e)))
    (is (= {:details "here"} (r/error->detail e)))))

(deftest errors->line-column-test
  (let [text     "foo\n\nbar"
        expected #{{:at 0 :line 1 :column 1}
                   {:at 2 :line 1 :column 3}
                   {:at 3 :line 1 :column 4}
                   {:at 4 :line 2 :column 1}
                   {:at 5 :line 3 :column 1}
                   {:at 8 :line 3 :column 4}}]
    (is (= expected (r/errors->line-column text (set/project expected [:at]))))))

(deftest success->texts
  (let [result   [nil {:start 0, :end 3} [:foo {:start 0, :end 3}]]
        expected [nil {:start 0, :end 3} [:foo "foo"]]]
    (is (= expected (r/success->texts "foo" result #{:foo})))))
