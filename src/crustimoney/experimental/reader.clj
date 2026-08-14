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
    "Indicate that no data before `index` will be requested anymore, 
    so those resources can be released."))

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

(defmacro ^:private if-bb [then else]
  (if (System/getProperty "babashka.version") then else))

(defn- read-chunk [{:keys [^Reader reader ^StringBuilder buffer chunk-size hit-end?]}]
  (when-not @hit-end?
    (let [carr (char-array chunk-size)
          read (.read reader carr)]
      (if (= read -1)
        (reset! hit-end? true)
        (.append buffer carr 0 read)))))

(defn- rb-length [{:keys [^StringBuilder buffer cut-at]}]
  (+ (.length buffer) @cut-at))

(defn- fill-buffer-to-index [{:keys [hit-end?] :as rb} index]
  (while (and (<= (rb-length rb) index) (not @hit-end?))
    (read-chunk rb)))

(defn- rb-char-at [{:keys [^StringBuilder buffer cut-at] :as rb} index]
  (fill-buffer-to-index rb index)
  (.charAt buffer (- index @cut-at)))

(defn- rb-sub-sequence [rb start end]
  (let [sub-buffer (StringBuilder.)]
    (doseq [i (range start end)]
      (.append sub-buffer (rb-char-at rb i)))
    (str sub-buffer)))

(defn- rb-match-literal [rb index string]
  (let [end (+ index (count string))]
    (fill-buffer-to-index rb (dec end))
    (when (and (<= end (rb-length rb))
               (= (rb-sub-sequence rb index end) string))
      (r/->success index end))))

(defn- rb-match-pattern [{:keys [^StringBuilder buffer hit-end? cut-at] :as rb} index pattern]
  (fill-buffer-to-index rb index)
  (when (< index (rb-length rb))
    (let [offset  @cut-at
          matcher (re-matcher pattern buffer)]
      (loop []
        (.region matcher (- index offset) (.length buffer))
        (let [found? (.lookingAt matcher)
              more?  (and (.hitEnd matcher) (not @hit-end?))]
          (if more?
            (do (read-chunk rb) (recur))
            (when found?
              (r/->success index (+ offset (.end matcher))))))))))

(defn- rb-cut [{:keys [^StringBuilder buffer cut-at]} index]
  (.delete buffer 0 (int (- index @cut-at)))
  (reset! cut-at index))

(if-bb
 (defrecord ReaderBuffer [reader buffer chunk-size hit-end? cut-at]
   Object
   (toString [_]
     (str buffer))

   r/TextSupport
   (sub-text [this start end]
     (rb-sub-sequence this start end))

   MatchSupport
   (match-literal [this index string]
     (rb-match-literal this index string))

   (match-pattern [this index pattern]
     (rb-match-pattern this index pattern))

   CutSupport
   (cut [this index]
     (rb-cut this index)))

 (defrecord ReaderBuffer [^Reader reader ^StringBuilder buffer chunk-size hit-end? cut-at]
   CharSequence
   (length [this]
     (rb-length this))

   (charAt [this index]
     (rb-char-at this index))

   (subSequence [this start end]
     (rb-sub-sequence this start end))

   (toString [_]
     (str buffer))

   r/TextSupport
   (sub-text [this start end]
     (rb-sub-sequence this start end))

   MatchSupport
   (match-literal [this index string]
     (rb-match-literal this index string))

   (match-pattern [this index pattern]
     (rb-match-pattern this index pattern))

   CutSupport
   (cut [this index]
     (rb-cut this index))))

(defn wrap-reader
  "Wrap a `Reader` to create a buffering `CharSequence` implementation
  which satisfies `MatchSupport` and `CutSupport` protocols. This
  makes it suitable as input for the `core/parse` function."
  [reader chunk-size]
  (map->ReaderBuffer
   {:reader     reader
    :chunk-size chunk-size
    :buffer     (StringBuilder.)
    :hit-end?   (atom false)
    :cut-at     (atom 0)}))

(defn reader?
  "Returns true if `obj` is a java `Reader`."
  [obj]
  (instance? Reader obj))
