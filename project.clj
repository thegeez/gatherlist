(defproject net.thegeez/gatherlist "0.0.1"
  :dependencies [[org.clojure/clojure "1.7.0"]

                 [com.stuartsierra/component "0.2.1"]

                 [org.clojure/java.jdbc "0.3.0"]
                 [postgresql/postgresql "8.4-702.jdbc4"]

                 [hiccup "1.0.5"]
                 [enlive "1.1.5"]

                 [buddy/buddy-hashers "0.4.2"]

                 [org.webjars/bootstrap "3.3.4"]
                 [org.webjars/jquery "1.11.1"]

                 [net.thegeez/w3a "0.0.3"]

                 [environ "1.0.1"]
                 [clj-time "0.11.0"]

                 [clj-http "2.0.0"]
                 [clj-oauth "1.5.3"]]
  :min-lein-version "2.0.0"
  :resource-paths ["config", "resources"]
  :profiles {:dev {:source-paths ["dev"]
                   :main user
                   :dependencies [[ns-tracker "0.2.2"]
                                  [org.clojure/tools.namespace "0.2.3"]
                                  [org.clojure/java.classpath "0.2.0"]
                                  [org.apache.derby/derby "10.8.1.2"]
                                  ;;[kerodon "0.6.0-SNAPSHOT"]
                                  [peridot "0.3.1" :exclusions [clj-time]]]}
             :uberjar {:main net.thegeez.gatherlist.main
                       :aot [net.thegeez.gatherlist.main]
                       :uberjar-name "gatherlist-prod-standalone.jar"}})
