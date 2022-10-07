(ns crustimoney2.results
  "Result constructors and predicates")

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

(defn ->push
  "Create a push value, given a parser function and an index. Optionally
  a state object can be added."
  ([parser index]
   (->push parser index nil))
  ([parser index state]
   {:parser parser :index index :state state}))

(defn push?
  "Returns obj if obj is a push value."
  [obj]
  (when (map? obj)
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
