(ns crustimoney.results
  "Result constructors, accessors and predicates")

;;; Success functions

(defn ->success
  "Create a success result, given a start index (inclusive) and end
  index (exclusive). Optionally a collection of success children can
  be given. The name of the success is nil."
  ([start end]
   [nil {:start start :end end}])
  ([start end children]
   (into (->success start end) children)))

(defn success?
  "Returns obj if obj is a success value, nil otherwise."
  [obj]
  (when (vector? obj)
    obj))

(defn success->start
  "Return the start index of a success."
  [success]
  (-> success second :start))

(defn success->end
  "Return the end index of a success."
  [success]
  (-> success second :end))

(defn success->children
  "Returns the children of a success."
  [success]
  (drop 2 success))

(defn success->name
  "Return the name of a success."
  [success]
  (first success))

(defn success->text
  "Returns the matched text of a success, given the full text."
  [text success]
  (subs text (success->start success) (success->end success)))

(defn ^:no-doc with-success-children
  "Set the children of a success."
  [success children]
  (let [[name attrs] success]
    (-> (into [name attrs] children)
        (with-meta (meta success)))))

(defn ^:no-doc with-success-name
  "Set the name of the success value."
  [key success]
  (-> (vec (cons key (rest success)))
      (with-meta (meta success))))

;;; Error functions

(defn ->error
  "Create an error result, given an error key and an index. An extra
  detail object can be added."
  ([key index]
   {:key key :at index})
  ([key index detail]
   {:key key :at index :detail detail}))

(defn error->key
  "Return the key of an error."
  [error]
  (error :key))

(defn error->index
  "Return the index of an error"
  [error]
  (error :at))

(defn error->detail
  "Return the detail object of an error."
  [error]
  (error :detail))

;;; Push functions

(defn ->push
  "Create a push value, given a parser function and an index. Optionally
  a state object can be added."
  ([parser index]
   (->push parser index nil))
  ([parser index state]
   {:op :push :parser parser :index index :state state}))

(defn push?
  "Returns obj if obj is a push value."
  [obj]
  (when (and (map? obj) (= (:op obj) :push))
    obj))

(defn push->parser
  "Returns the parser of a push value."
  [push]
  (push :parser))

(defn push->index
  "Returns the index of a push value."
  [push]
  (push :index))

(defn push->state
  "Returns the state of a push value."
  [push]
  (push :state))

;;; Line and columns for errors

(defn- indices->line-columns [text indices]
  (loop [indices   (sort (distinct indices))
         line-cols {}
         cursor    0
         line      1
         col       1]
    (if-let [index (first indices)]
      (let [hit?     (= cursor index)
            newline? (= (nth text cursor :eof) \newline)]
        (recur (cond-> indices hit? rest)
               (cond-> line-cols hit? (assoc index {:line line :column col}))
               (inc cursor)
               (cond-> line newline? inc)
               (if newline? 1 (inc col))))
      line-cols)))

(defn errors->line-column
  "Returns the errors with `:line` and `:column` entries added."
  [text errors]
  (let [grouped   (group-by error->index errors)
        line-cols (indices->line-columns text (keys grouped))]
    (->> (map #(merge %1 (line-cols (error->index %1))) errors)
         (set))))
