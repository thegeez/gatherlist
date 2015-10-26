(defproject net.thegeez/gatherlist "0.0.1"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.48"]

                 [com.stuartsierra/component "0.2.1"]

                 [org.clojure/java.jdbc "0.3.0"]
                 [postgresql/postgresql "8.4-702.jdbc4"]

                 [hiccup "1.0.5"]
                 [enlive "1.1.5"]

                 [buddy/buddy-hashers "0.4.2"]

                 [org.webjars/bootstrap "3.3.4"]
                 [org.webjars/jquery "1.11.1"]

                 [net.thegeez/w3a "0.0.4"]

                 [clj-time "0.11.0"]]

  :plugins [[lein-cljsbuild "1.1.0"]]

  :min-lein-version "2.0.0"
  :resource-paths ["config", "resources"]

  :source-paths ["src/clj"]
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
                       :uberjar-name "gatherlist-prod-standalone.jar"}}

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/cljs"]
                        :compiler {:output-to "resources/public/js/gatherlist_dev.js"
                                   :main 'net.thegeez.gatherlist.client
                                   :optimizations :whitespace}
                        :notify-command ["notify-send" "cljsbuild"]}
                       {:id "prod"
                        :source-paths ["src/cljs"]
                        :compiler
                        {:output-to "resources/public/js/gatherlist.js"
                         :main 'net.thegeez.gatherlist.client
                         :optimizations :advanced}
                        :notify-command ["notify-send" "cljsbuild"]}]})
