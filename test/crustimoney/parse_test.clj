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

(fact "the first succesful choice in a vector is used"
  (parse {:a [:b1 / :b2] :b1 \b :b2 \b} :a "b") => {:succes {:b1 "b"}}
  (parse {:a [:b / :c] :b \b :c \c} :a "c") => {:succes {:c "c"}})

(fact "recurring rules are put in a vector"
  (parse {:a [:b :a / \c] :b \b} :a "bbc") => {:succes [{:b "b"} {:b "b"}]})

(fact "with-spaces adds mandatory spaces between items"
  (parse {:a (with-spaces \a \b)} :a "a b") => {:succes nil}
  (parse {:a (with-spaces \a \b)} :a "ab") => (just {:error anything}))

(fact "minus sign in rule name makes it a terminal"
  (parse {:a [:b] :b- [:c :b / :c] :c \c} :a "ccc") => {:succes {:b "ccc"}})

(fact "errors give the correct line and column number"
  (parse {:a (with-spaces \a \b)} :a "a") =>
    (just {:error (contains {:line 1 :column 2})})
  (parse {:a (with-spaces \a \b)} :a "a\n  ") =>
    (just {:error (contains {:line 2 :column 3})}))
