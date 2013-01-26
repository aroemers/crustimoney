(defproject crustimoney "0.1.0-SNAPSHOT"
  :description "A Clojure idiomatic PEG parser."
  :url "http://github.com/aroemers/crustimoney"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]]
  :repl-options {:init-ns crustimoney.parse}
  :profiles {:dev {:dependencies [[midje "1.5-alpha9"]]
                   :plugins [[lein-midje "3.0-alpha4"]]}})
