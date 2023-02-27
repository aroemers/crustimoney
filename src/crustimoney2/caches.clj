(ns crustimoney2.caches
  "Packrat caches for the core/parse function.

  Caches are implemented using the Cache protocol. This way you can
  implement your own if desired.")

(defprotocol Cache
  "Protocol for packrat cache implementations."

  (fetch [this text parser index]
    "Try to fetch a cached result, returns nil if it misses the cache.")

  (store [this text parser index result]
    "Store a result in the cache."))

;;; Provided implementations.

(def ^:no-doc noop-cache
  (reify Cache
    (fetch [_ _ _ _])
    (store [_ _ _ _ _])))

(defn atom-cache
  "Create a cache that uses a plain atom for storage, without any
  eviction (until it is garbage collected).

  This cache is *not* suitable for parsing multiple texts. Create a
  new cache for each text."
  []
  (let [cache (atom nil)]
    (reify Cache
      (fetch [_ text parser index]
        (get-in @cache [parser index]))

      (store [_ text parser index result]
        (swap! cache assoc-in [parser index] result)))))
