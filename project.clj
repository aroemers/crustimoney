(defproject functionalbytes/crustimoney :lein-v
  :description "A Clojure PEG parser, with packrat and cut"
  :url "https://github.com/aroemers/crustimoney"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}

  :dependencies [[org.clojure/clojure "1.11.1"]]

  :repl-options {:init-ns crustimoney.core}

  :plugins [[lein-cloverage "1.2.4"]
            [lein-eftest "0.6.0"]
            [com.roomkey/lein-v "7.2.0"]]

  :deploy-repositories [["clojars" {:username      :env/CLOJARS_USERNAME
                                    :password      :env/CLOJARS_TOKEN
                                    :sign-releases false}]]

  :profiles
  {:dev {:dependencies [[instaparse/instaparse "1.4.12"]
                        [com.clojure-goes-fast/clj-memory-meter "0.2.2"]]

         :jvm-opts [;; For clj-memory-meter
                    "-Djdk.attach.allowAttachSelf"]}

   :test {:dependencies [[cloverage/cloverage "1.2.4"]]

          :cloverage {:fail-threshold 90}}})
