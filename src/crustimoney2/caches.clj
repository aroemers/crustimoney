(ns crustimoney2.caches
  "Packrat caches for the core/parse function.

  Caches are implemented by functions that take three and four
  arguments. The function is called with three arguments (text,
  parser, index) to try and fetch a cached result. It returns nil if
  it misses the cache. The function is called with four
  arguments (text, parser, index, result) to store a result. It
  returns the result back.")

(defn atom-cache
  "Create a cache that uses a plain atom for storage, without any
  eviction (until it is garbage collected that is)."
  []
  (let [a (atom nil)]
    (fn
      ([_text parser index]
       (get-in @a [parser index]))
      ([_text parser index result]
       (swap! a assoc-in [parser index] result)
       result))))

(defn clojure-core-cache
  "Create a cache by wrapping a clojure.core.cache cache.

  This function resolves the clojure.core.cache library dynamically;
  you'll need to add the dependency to it yourself."
  [cache]
  (let [has? (requiring-resolve 'clojure.core.cache/has?)
        hit  (requiring-resolve 'clojure.core.cache/hit)
        miss (requiring-resolve 'clojure.core.cache/miss)]
    (fn
      ([_text parser index]
       (when (has? cache [parser index])
         (hit cache [parser index])))
      ([_text parser index result]
       (miss cache [parser index] result)))))
