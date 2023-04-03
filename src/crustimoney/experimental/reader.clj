(ns crustimoney.experimental.reader
  "This experimental reader is created for stream support.

  It takes a `Reader` as input and implements a `CharSequence`, based
  on an internal buffer. It will fill its buffer on demand.

  Support for matching a literal substring or a regular expression
  pattern has been implemented on top of this, via the `MatchSupport`
  protocol. Part of the internal buffer can be released by a cut
  operation, via the `CutSupport` protocol.

  The `MatchSupport` protocol has been implemented for a normal String
  as well."
  (:require [crustimoney.results :as r])
  (:import [java.io Reader]))

;;; The protocols

(defprotocol MatchSupport
  (match-literal [this index string]
    "Try to match the given `string` literal on `index`. Returns a
    success node, or nil.")

  (match-pattern [this index pattern]
    "Try to match the given regular expression `pattern` on `index`.
    Returns a success node, or nil."))

(defprotocol CutSupport
  (cut [this index]
    "Clear the internal buffer before the given `index`."))

;;; String implementation

(extend-type java.lang.String
  MatchSupport
  (match-literal [this index s]
    (when (.startsWith this s index)
      (r/->success index (+ index (count s)))))

  (match-pattern [this index pattern]
    (let [matcher (re-matcher pattern this)]
      (.region matcher index (count this))
      (when (.lookingAt matcher)
        (r/->success index (.end matcher))))))

;;; Caching reader implementation

(defn- read-chunk [{:keys [^Reader reader ^StringBuilder buffer chunk-size hit-end?]}]
  (when-not @hit-end?
    (let [carr (char-array chunk-size)
          read (.read reader carr)]
      (if (= read -1)
        (reset! hit-end? true)
        (.append buffer carr 0 read)))))

(defn- fill-buffer-to-index [{:keys [hit-end?] :as rb} index]
  (while (and (<= (.length ^CharSequence rb) index) (not @hit-end?))
    (read-chunk rb)))

(defrecord ReaderBuffer [^Reader reader ^StringBuilder buffer chunk-size hit-end? cut-at]
  CharSequence
  (length [_]
    (+ (count buffer) @cut-at))

  (charAt [this index]
    (fill-buffer-to-index this index)
    (.charAt buffer (- index @cut-at)))

  (subSequence [this start end]
    (let [sub-buffer (StringBuilder.)]
      (doseq [i (range start end)]
        (.append sub-buffer (.charAt this i)))
      (str sub-buffer)))

  (toString [_]
    (str buffer))

  MatchSupport
  (match-literal [this index string]
    (let [end (+ index (count string))]
      (fill-buffer-to-index this (dec end))
      (when (and (<= end (.length this))
                 (= (.subSequence this index end) string))
        (r/->success index end))))

  (match-pattern [this index pattern]
    (fill-buffer-to-index this index)
    (when (< index (.length this))
      (let [matcher (re-matcher pattern this)]
        (loop []
          (.region matcher index (.length this))
          (let [found? (.lookingAt matcher)
                more?  (and (.hitEnd matcher) (not @hit-end?))]
            (if more?
              (do (read-chunk this) (recur))
              (when found?
                (r/->success index (.end matcher)))))))))

  CutSupport
  (cut [_ index]
    (.delete buffer 0 (int (- index @cut-at)))
    (reset! cut-at index)))

(defn wrap-reader
  "Wrap a `Reader` to create a buffering `CharSequence` implementation
  which satisfies `MatchSupport` and `CutSupport` protocols. This
  makes it suitable as input for the `core/parse` function."
  [reader chunk-size]
  (map->ReaderBuffer
   {:reader     reader
    :buffer     (StringBuilder.)
    :chunk-size chunk-size
    :hit-end?   (atom false)
    :cut-at     (atom 0)}))

(defn reader?
  "Returns true if `obj` is a java `Reader`."
  [obj]
  (instance? Reader obj))
