(ns data-readers-test
  (:require [clojure.test :refer [deftest is]]))

(deftest crusti-parser-test
  (is (= [:literal "foo"] #crusti/parser "foo")))

(deftest crusti-regex-test
  (is (instance? java.util.regex.Pattern #crusti/regex "[a-z]")))
