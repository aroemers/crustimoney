;;;; Internal parsing functions.
;;;;
;;;; This namespace contains the internal parse functions. These are not
;;;; intended to be called directly by the user.

(ns pegparser.internal.core
  (:use [pegparser.internal.utils]))


;;; The data structures used by the parse functions.

;; The State record is merely here for documentation purposes of what the state
;; contains, and may yield some performance gains.
(defrecord State
  [rules remainder pos current as-terminal errors errors-pos])

(defn succes
  "Make a new succes map based on the parameters."
  [content new-state]
  {:succes (mapify content new-state)})

(defn error
  "Given an error `content` string and the current state, a map is returned with
  the `:errors` and the `:errors-pos`. Based on the given state, only the
  \"deepest\" errors remain."
  [content {:keys [pos errors errors-pos] :as state}]
  (let [[errors errors-pos] (cond (= pos errors-pos) [(conj errors content) errors-pos]
                                  (> pos errors-pos) [#{content} pos]
                                  :else [errors errors-pos])]
    (mapify errors errors-pos)))


;;; The vector parsing functions.

(declare parse-terminal parse-nonterminal parse-vector)

(defn init-vector-result
  "Initialise a data structure for storing the result during the parsing of
  a vector."
  []
  [nil nil])

(defn add-to-vector-result
  "Given a vector `item`, the current `vector-result`, the content of a
  succesful parse result and the current `state`, this function returns an
  updated vector-result."
  [item vector-result content {:keys [current as-terminal] :as state}]
  (if as-terminal
    [nil (str (second vector-result) content)]
    (cond (vector? item)
          (if (vector? content)
            [(first vector-result) (into [] (concat content (second vector-result)))]
            [(merge (first vector-result) content) (second vector-result)])
          (keyword? item)
          (if (= current item)
            [(first vector-result) (if (vector? content)
                              content
                              (if (empty? content) [] (vector content)))]
            [(assoc (first vector-result) item content) (second vector-result)])
          :else vector-result)))

(defn vector-result-to-succes
  "Convert a `vector-result` datastructure to a succes structure, using the
  `succes` function."
  [vector-result state]
  (succes (if (second vector-result)
            (if (empty? (first vector-result))
              (second vector-result)
              (into [] (cons (first vector-result) (second vector-result))))
            (first vector-result))
          state))

(defn parse-vector-item
  [item state]
  (let [parse-fn (cond (vector? item) parse-vector
                       (keyword? item) parse-nonterminal
                       :else parse-terminal)]
    (parse-fn item state)))

(defn parse-vector
  [vect {:keys [current as-terminal] :as state}]
  (loop [vect vect
         new-state state
         vector-result (init-vector-result)]
    (let [item (first vect)]
      (if (or (nil? item) (= item /))
        (vector-result-to-succes vector-result new-state)
        (let [parse-result (parse-vector-item item new-state)]
          (if-let [succes (:succes parse-result)]
            (let [new-state (:new-state succes)
                  new-vector-result (add-to-vector-result item
                                                          vector-result
                                                          (:content succes)
                                                          new-state)]
              (recur (rest vect) new-state new-vector-result))
            (let [next-choice (drop-while #(not (= / %)) vect)]
              (if (empty? next-choice)
                parse-result
                (recur (rest next-choice)
                       (merge state parse-result)
                       (init-vector-result))))))))))

(defn regex? [v]
  (instance? java.util.regex.Pattern v))

(defn parse-terminal
  [expression {:keys [remainder pos] :as state}]
  (let [[result match-type-str] (cond
          (char? expression) [(when (= expression (first remainder)) (str expression)) "character"]
          (string? expression) [(when (.startsWith remainder expression) expression) "string"]
          (regex? expression) [(when-let [match (re-find (re-pattern (str "^" (.pattern expression)))
                                                         remainder)]
                                 (if (vector? match) (first match) match))
                               "a character sequence that matches"]
          :else (throw (Exception. (format "An instance of %s is not a valid parsing expression."
                                           (class expression)))))]
    (if result
      (succes result (assoc state :remainder (subs remainder (count result))
                                  :pos (+ pos (count result))))
      (error (format "expected %s '%s'" match-type-str expression) state))))

(defn parse-nonterminal
  [nonterminal {:keys [rules as-terminal] :as state}]
  (let [expression (or (rules nonterminal)
                       (rules (keyword (str (name nonterminal) \-))))
        as-terminal (or as-terminal (rules (keyword (str (name nonterminal) \-))))]
    (if (vector? expression)
      (parse-vector expression (assoc state :current nonterminal
                                            :as-terminal as-terminal))
      (parse-terminal expression state))))

(defn line-and-column
  [pos text]
  (let [text (subs text 0 pos)
        line (inc (count (filter #(= \newline %) text)))
        column (inc (count (take-while #(not (= \newline %)) (reverse text))))]
    [line column]))