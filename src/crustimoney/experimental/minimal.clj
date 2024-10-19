(ns crustimoney.experimental.minimal
  (:require [crustimoney.caches :as caches]))

;;; initial state
(def init
  {:txt ""
   :pos 0
   :cut 0
   :res nil
   :err nil})

;;; helpers
(defn ok? [result]
  (not (:err result)))

;;; combinators
(defn wrap-caching [parser]
  (fn [state]
    (if-let [hit (caches/fetch (:cache state) parser (:pos state))]
      hit
      (let [result (parser state)]
        (caches/store (:cache state) parser (:pos state) result)
        result))))

(defn with-name [key parser]
  (fn [state]
    (let [result (parser state)]
      (if (ok? result)
        (assoc result :res [(into [key {:start (:pos state), :end (:pos result)}]
                                  (:res result))])
        result))))

(defn literal [text]
  (fn [state]
    (if (.startsWith (:txt state) text (:pos state))
      (assoc state :pos (+ (:pos state) (count text)))
      (assoc state :err #{{:key :expected-literal, :detail {:text text}}}))))

(defn chain [& parsers]
  (fn [state]
    (-> (fn [state parser]
          (let [result (parser (dissoc state :res))]
            (if (ok? result)
              (assoc result :res (concat (:res state) (:res result)))
              (if (:soft-cut? result)
                (throw (ex-info "soft cut" result))
                (reduced result)))))
        (reduce state parsers)
        (dissoc :soft-cut?))))

(defn choice [& parsers]
  (fn [state]
    (reduce (fn [state parser]
              (let [result (parser state)]
                (if (ok? result)
                  (reduced result)
                  (if (< (:pos state) (:cut result)) ;; put this in wrapper?
                    (throw (ex-info "hard cut" result))
                    result))))
            state parsers)))

(defn repeat* [parser]
  (fn [state]
    (loop [state state]
      (let [result (parser (dissoc state :res))]
        (if (ok? result)
          (recur (assoc result :res (concat (:res state) (:res result))))
          state)))))

(defn negate [parser]
  (fn [state]
    (let [result (parser state)]
      (if (ok? result)
        (assoc state :err #{{:key :unexpected-match, :detail {:text (subs (:txt state) (:pos state) (:pos result))}}})
        state))))

(def hard-cut
  (fn [state]
    (assoc state :cut (:pos state))))

(def soft-cut
  (fn [state]
    (assoc state :soft-cut? true)))

(defn parse [parser text]
  (parser (assoc init :txt text)))

(comment
  (def parser (with-name :root
                (chain (with-name :foo
                         (literal "foo"))
                       soft-cut
                       (negate (literal "baz"))
                       (repeat* (with-name :bax
                                  (choice (chain (literal "ba")
                                                 ;; hard-cut
                                                 (literal "r"))
                                          (literal "baz")))))))

  (parser init)

  (parser (assoc init :txt "foobarbazbax"))

  (parser (assoc init :txt "foobazbar"))

  (parser (assoc init :txt "alice")))
