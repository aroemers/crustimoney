(ns crustimoney.caches-test
  (:require [clojure.test :refer [deftest testing is]]
            [crustimoney.caches :as cache]))

(deftest treemap-cache-test
  (testing "miss, hit and cut"
    (let [cache  (cache/treemap-cache)
          parser (fn [])]
      (is (nil? (cache/fetch cache parser 5)))
      (cache/store cache parser 5 :result)
      (is (= :result (cache/fetch cache parser 5)))
      (cache/cut cache 5)
      (is (= :result (cache/fetch cache parser 5)))
      (cache/cut cache 6)
      (is (nil? (cache/fetch cache parser 5))))))

(deftest weak-treemap-cache-test
  (testing "miss, hit and cut"
    (let [cache    (cache/weak-treemap-cache)
          parser-1 (fn [])
          parser-2 (fn [])]
      (is (nil? (cache/fetch cache parser-1 5)))
      (is (nil? (cache/fetch cache parser-2 5)))
      (cache/store cache parser-1 5 :result-1)
      (cache/store cache parser-2 5 :result-2)
      (is (= :result-1 (cache/fetch cache parser-1 5)))
      (is (= :result-2 (cache/fetch cache parser-2 5)))
      (cache/cut cache 5)
      (is (= :result-1 (cache/fetch cache parser-1 5)))
      (is (= :result-2 (cache/fetch cache parser-2 5)))
      (cache/cut cache 6)
      (is (nil? (cache/fetch cache parser-1 5)))
      (is (nil? (cache/fetch cache parser-2 5))))))
