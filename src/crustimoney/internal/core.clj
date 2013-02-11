(ns crustimoney.internal.core
  "This namespace contains the internal parse functions. These are not
  intended to be called directly by the user."
  (:use [crustimoney.internal.utils]
        [crustimoney.i18n :only (i18n)]))


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
  ;; The first item in this tuple may hold the hash-map of the parsed vector
  ;; items. The second may hold a sequence of recuring parse results or a
  ;; string.
  [nil nil])

(defn add-to-vector-result
  "Given a vector `item`, the current `vector-result`, the `content` of a
  succesful parse result and the current `state`, this function returns an
  updated vector-result."
  [item vector-result content {:keys [current as-terminal] :as state}]
  ;; Check if the non-terminal should be regarded as a terminal. If so, then
  ;; append it to the second item of the tuple.
  (if as-terminal
    [nil (str (second vector-result) content)]
    ;; Add the content to the vector-result based on the type of vector item.
    (cond (vector? item)
          (if (vector? content)
            [(first vector-result) (into [] (concat content (second vector-result)))]
            [(merge (first vector-result) content) (second vector-result)])
          (keyword? item)
          (if (= current item)
            [(first vector-result)
             (if (vector? content)
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
  "Given an `item` from a vector parsing expression and the current `state`,
  call the correct parsing function based on the type of the item."
  [item state]
  (let [parse-fn (cond (vector? item) parse-vector
                       (keyword? item) parse-nonterminal
                       :else parse-terminal)]
    (parse-fn item state)))

(defn parse-vector
  "This function contains the main iteration through a vector parsing
  expression."
  [vect {:keys [current as-terminal] :as state}]
  ;; Start at the beginning of the vector, take the current state and
  ;; initialise a result data structure.
  (loop [vect vect
         new-state state
         vector-result (init-vector-result)]
    ;; Take the first item of the current vector and check whether we are
    ;; done parsing the current sequence of items.
    (let [item (first vect)]
      (if (or (nil? item) (= item /))
        (vector-result-to-succes vector-result new-state)
        ;; Not done yet, so parse the current item and check whether it
        ;; was a succes. If so, continue to the next item in the vector.
        (let [parse-result (parse-vector-item item new-state)]
          (if-let [succes (:succes parse-result)]
            (let [new-state (:new-state succes)
                  new-vector-result (add-to-vector-result item
                                                          vector-result
                                                          (:content succes)
                                                          new-state)]
              (recur (rest vect) new-state new-vector-result))
            ;; The parsing of the item failed, check if there is a choice
            ;; operator further up in the vector. If not, then parsing the
            ;; vector has failed. Otherwise, reset the state (without losing the
            ;; errors found), and try again with the next sequence.
            (let [next-choice (drop-while #(not (= / %)) vect)]
              (if (empty? next-choice)
                parse-result
                (recur (rest next-choice)
                       (merge state parse-result)
                       (init-vector-result))))))))))


;;; The terminal parsing functions.

(defn parse-terminal-expression
  "The function that tries to match the `expression` on (the start of) the
  `remainder`. If it matches, it returns the matched text. Otherwise, it returns
  nil."
  [expression remainder]
  (cond (char? expression) (when (= expression (first remainder)) (str expression))
        (string? expression) (when (.startsWith remainder expression) expression)
        (regex? expression)
          (when-let [match (re-find (re-pattern (str "^" (.pattern expression)))
                                                remainder)]
            (if (vector? match) (first match) match))
        :else (throw (Exception. (i18n :invalid-parsing-expr
                                       (class expression))))))

(defn terminal-expression-name
  "Returns the human readable name of the type of terminal parsing `expression`."
  [expression]
  (cond (char? expression) (i18n :char-terminal)
        (string? expression) (i18n :string-terminal)
        (regex? expression) (i18n :regex-terminal)))

(defn parse-terminal
  "The actual terminal parsing function. It returns a succes or an error, as
  defined by their respective functions."
  [expression {:keys [remainder pos] :as state}]
  (if-let [result (parse-terminal-expression expression remainder)]
    (succes result (assoc state :remainder (subs remainder (count result))
                                :pos (+ pos (count result))))
    (error (i18n :expected-terminal
                 (terminal-expression-name expression)
                 expression)
           state)))


;;; Namespace entry functions.

(defn parse-nonterminal
  "Parse the rule that in the rules map (in the `state`) as named by the keys
  `nonterminal`."
  [nonterminal {:keys [rules as-terminal] :as state}]
  (let [expression (or (rules nonterminal)
                       (rules (keyword (str (name nonterminal) \-))))
        as-terminal (or as-terminal (rules (keyword (str (name nonterminal) \-))))]
    (if (vector? expression)
      (parse-vector expression (assoc state :current nonterminal
                                            :as-terminal as-terminal))
      (parse-terminal expression state))))

(defn line-and-column
  "Given a `text` string and a position (starting from 0), return a tuple with
  the line and column number of that position in the text (both starting at 1)."
  [pos text]
  (let [text (subs text 0 pos)
        line (inc (count (filter #(= \newline %) text)))
        column (inc (count (take-while #(not (= \newline %)) (reverse text))))]
    [line column]))