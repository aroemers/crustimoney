(ns crustimoney2.caches
  "Packrat caches for the core/parse function.

  A single cache instance is *not* intended to be used for multiple
  texts. Create a new cache for each new text.

  Caches are implemented using the Cache protocol. This way you can
  implement your own, if desired.")

(defprotocol Cache
  "Protocol for packrat cache implementations."

  (fetch [this parser index]
    "Try to fetch a cached result, returns nil if it misses the cache.")

  (store [this parser index result]
    "Store a result in the cache."))

;;; Provided implementations.

(def ^:no-doc noop-cache
  (reify Cache
    (fetch [_ _ _])
    (store [_ _ _ _])))

(defn basic-cache
  "Create a cache that uses a plain map for storage, without any
  eviction (until it is garbage collected)."
  []
  (let [cache (volatile! nil)]
    (reify Cache
      (fetch [_ parser index]
        (get-in @cache [parser index]))

      (store [_ parser index result]
        (vswap! cache assoc-in [parser index] result)))))

(defn weak-cache
  "Create a cache that uses weak references, such that entries are
  evicted on memory pressure."
  []
  (let [cache (java.util.WeakHashMap.)]
    (reify Cache
      (fetch [_ parser index]
        (get cache [parser index]))

      (store [_ parser index result]
        (.put cache [parser index] result)))))
