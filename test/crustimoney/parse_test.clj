(ns crustimoney.parse-test
  "Testing functions for the API namespace of Crustimoney."
  (:use midje.sweet
        crustimoney.parse))


(facts "basic terminals are parsed and included in result"
  (parse {:a \a} :a "a") => {:succes "a"}
  (parse {:a "foo"} :a "foo") => {:succes "foo"}
  (parse {:a #"foo|bar"} :a "bar") => {:succes "bar"})

(fact "simple vectors are parsed, but not included in result"
  (parse {:a [\a "foo" #"foo|bar"]} :a "afoobar") => {:succes nil})

(fact "vector calling other terminal rule is included in result"
  (parse {:a [:b] :b \b} :a "b") => {:succes {:b "b"}})

(fact "inner vectors are parsed just as well"
  (parse {:a [[:b] :c] :b \b :c \c} :a "bc") => {:succes {:b "b" :c "c"}})