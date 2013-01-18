;;;; Utility functions.

(ns pegparser.internal.utils)

(defmacro mapify
  "Given some symbols, construct a map with the symbols as keys, and the value
  of the symbols as the map values. For example:

  (let [foo \"bar\"]
    (mapify foo))    ; => {:foo \"bar\"}

  From `frozenlock` on #clojure."
  [& symbols]
  `(into {}
         (filter second
                 ~(into []
                        (for [item symbols]
                          [(keyword item) item])))))