(ns crustimoney.caches
  "Packrat caches for the core/parse function.

  A single cache instance is *not* intended to be used for multiple
  texts. Create a new cache for each new text.

  Caches are implemented using the Cache protocol. This way you can
  implement your own, if desired."
  (:import [java.util HashMap TreeMap WeakHashMap]))

(defprotocol Cache
  "Protocol for packrat cache implementations."

  (fetch [this parser index]
    "Try to fetch a cached result, returns nil if it misses the cache.")

  (store [this parser index result]
    "Store a result in the cache.")

  (cut [this index]
    "Clear all cached results before given index."))

;;; Provided implementations.

(def ^:no-doc noop-cache
  (reify Cache
    (fetch [_ _ _])
    (store [_ _ _ _])
    (cut   [_ _])))

(defn treemap-cache
  "Create a cache that supports clearing below a certain index, such
  that entries are evicted on cuts."
  []
  (let [cache (java.util.TreeMap.)]
    (reify Cache
      (fetch [_ parser index]
        (get-in cache [(int index) parser]))

      (store [_ parser index result]
        (if-let [parsers (get cache (int index))]
          (.put ^HashMap parsers parser result)
          (let [parsers (HashMap.)]
            (.put parsers parser result)
            (.put cache (int index) parsers))))

      (cut [_ index]
        (.. cache (headMap (int index)) clear)))))

(defn weak-treemap-cache
  "Create a cache that supports clearing below a certain index and has
  weak references, such that entries are evicted on cuts or on memory
  pressure."
  []
  (let [cache (TreeMap.)]
    (reify Cache
      (fetch [_ parser index]
        (get-in cache [(int index) parser]))

      (store [_ parser index result]
        (if-let [parsers (get cache (int index))]
          (.put ^WeakHashMap parsers parser result)
          (let [parsers (WeakHashMap.)]
            (.put parsers parser result)
            (.put cache (int index) parsers))))

      (cut [_ index]
        (.. cache (headMap (int index)) clear)))))
