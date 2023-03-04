(ns crustimoney2.results
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

(defn with-success-children
  "Set the children of a success."
  [success children]
  (let [[name attrs] success]
    (into [name attrs] children)))

(defn success->children
  "Returns the children of a success."
  [success]
  (drop 2 success))

(defn with-success-name
  "Set the name of the success value."
  [key success]
  (vec (cons key (rest success))))

(defn success->name
  "Return the name of a success."
  [success]
  (first success))

(defn with-success-attrs
  "Add extra success attributes to the given success."
  [success attrs]
  (update success 1 merge attrs))

(defn success->attrs
  "Return the attributes of a success."
  [success]
  (dissoc (second success) :start :end))

(defn success->attr
  "Returns an attribute value of a success."
  [success attr]
  (get (second success) attr))

(defn success->text
  "Returns the matched text of a success, given the full text."
  [success text]
  (subs text (success->start success) (success->end success)))

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
   {:push {:parser parser :index index :state state}}))

(defn push?
  "Returns obj if obj is a push value."
  [obj]
  (when (and (map? obj) (:push obj))
    obj))

(defn push->parser
  "Returns the parser of a push value."
  [push]
  (get-in push [:push :parser]))

(defn push->index
  "Returns the index of a push value."
  [push]
  (get-in push [:push :index]))

(defn push->state
  "Returns the state of a push value."
  [push]
  (get-in push [:push :state]))

;;; Cut functions

(defn ->cut
  "Wrap the given result with a cut."
  [result]
  {:cut {:result result}})

(defn cut?
  "Returns obj if obj is a cut value."
  [obj]
  (when (and (map? obj) (:cut obj))
    obj))

(defn cut->result
  "Returns the wrapped result of a cut."
  [cut]
  (get-in cut [:cut :result]))

;;; Line and columns for errors

(defn- line-breaks-in [text]
  (let [length (count text)]
    (loop [index   0
           lengths (transient [])
           current 0]
      (if (< index length)
        (if (= (nth text index) \newline)
          (recur (inc index) (conj! lengths current) 0)
          (recur (inc index) lengths (inc current)))
        (persistent! lengths)))))

(defn- index->line-column [line-breaks index]
  (loop [at     index
         line   1
         breaks line-breaks]
    (if-let [break (first breaks)]
      (if (< (- at break) 0)
        {:line line, :column (inc at)}
        (recur (- at break) (inc line) (rest breaks)))
      {:line line, :column (inc at)})))

(defn errors->line-column
  "Returns the errors with `:line` and `:column` entries added."
  [errors text]
  (let [line-breaks (line-breaks-in text)]
    (mapcat (fn [[at errors]]
              (let [lc (index->line-column line-breaks at)]
                (map (partial merge lc) errors)))
            (group-by error->index errors))))

;;; Keep furthers errors

(defn errors->furthest
  "Returns only the errors that have the highest index."
  [errors]
  (let [grouped (group-by error->index errors)]
    (get grouped (apply max (keys grouped)))))
