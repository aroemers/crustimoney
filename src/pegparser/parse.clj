(ns pegparser.parse
  (:require [pegparser.internal.core :as core]))

(defn parse
  [rules start text]
  (core/parse-nonterminal start (core/map->State {:rules rules
                                                  :remainder text
                                                  :pos 0
                                                  :errors #{}
                                                  :errors-pos 0})))

(defn with-spaces
  [& items]
  (into [] (interpose #"\s+" items)))

(def rules
  {:root          [ #"\s*\Z" / #"\s*" :toplevel :root ]
   :toplevel      [ :association / :precedence ]

   :association   [ (with-spaces "associate" :where "with" :what) [ :using / ] ]
   :where         [ :where-field ]
   :what          [ :what-new / :what-single / :what-retval ]
   :using         [ #"\s+" (with-spaces "using" :fqn) ]

   :where-field   (with-spaces "field" :fqn)

   :what-new      (with-spaces "new" :fqn)
   :what-single   (with-spaces "single" :fqn)
   :what-retval   [ (with-spaces "retval" :fqn) :parameters ]

   :parameters-   [ :paren-open :param-exprs :paren-close ]
   :param-exprs   [ :param-expr :param-exprs / ]
   :param-expr    [ :non-paren / :paren-open :param-exprs :paren-close ]
   :non-paren     #"[^\(\)]"
   :paren-open    \(
   :paren-close   \)

   :precedence    (with-spaces "declare" "precedence" :prec-items)
   :prec-items    [ :fqn #"\s*,\s*" :prec-items / :fqn ]

   :fqn           #"[A-Za-z0-9\.\$_]+" })

; (def rules
;   {:root          [ #"\s*\Z" / #"\s*" :toplevel :root ]
;    :toplevel      [ :association / :precedence ]

;    :association   [ (with-spaces "associate" :where "with" :what) :using? ]
;    :where         [ :where-field ]
;    :what          [ :what-new / :what-single / :what-retval ]
;    :using         [ #"\s+" (with-spaces "using" :fqn) ]

;    :where-field   (with-spaces "field" :fqn)

;    :what-new      (with-spaces "new" :fqn)
;    :what-single   (with-spaces "single" :fqn)
;    :what-retval   [ (with-spaces "retval" :fqn) :parameters ]

;    :parameters-   [ :paren-open :param-expr* :paren-close ]
;    :param-expr    [ :non-paren / [ :paren-open :param-expr* :paren-close ] ]
;    :non-paren     #"[^\(\)]"
;    :paren-open    \(
;    :paren-close   \)

;    :precedence    (with-spaces "declare" "precedence" :prec-items)
;    :prec-items    [ [ :fqn #"\s*,\s*" :prec-items ] / :fqn ]

;    :fqn           #"[A-Za-z0-9\.\$_]+" })
