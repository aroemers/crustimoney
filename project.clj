(defproject functionalbytes/crustimoney "2.0.0"
  :description "A Clojure PEG parser, with packrat and cut"
  :url "https://github.com/aroemers/crustimoney"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}

  :dependencies [[org.clojure/clojure "1.11.1"]]

  :repl-options {:init-ns crustimoney2.core}

  :profiles
  {:dev {:dependencies [[instaparse/instaparse "1.4.12"]
                        [com.clojure-goes-fast/clj-memory-meter "0.2.2"]]

         :jvm-opts ["-Djdk.attach.allowAttachSelf"]}})
