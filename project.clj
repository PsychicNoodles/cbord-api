(defproject cbord-api "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [enlive "1.1.6"]
                 [clj-http "2.3.0"]
                 [slingshot "0.12.2"]
                 [http-kit "2.2.0"]
                 [compojure "1.5.2"]
                 [ring/ring-defaults "0.2.1"]
                 [ring/ring-json "0.4.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [ring-logger "0.7.7"],,]
  :main ^:skip-aot cbord-api.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
