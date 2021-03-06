(defproject cbord-api "1.0.0"
  :description "Provides access to the CBORD Get dining services web application through JSON for easier development of scripts and apps."
  :url "http://github.com/PsychicNoodles/cbord-api"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [enlive "1.1.6"]
                 [clj-http "2.3.0"]
                 [slingshot "0.12.2"]
                 [http-kit "2.2.0"]
                 [compojure "1.5.2"]
                 [ring/ring-defaults "0.2.1"]
                 [ring/ring-json "0.4.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [ring-logger "0.7.7"]
                 [pdfboxing "0.1.11"]
                 [expiring-map "0.1.7"]
                 [environ "1.1.0"]
                 [clj-time "0.13.0"]]
  :min-lein-version "2.5.1"
  :main ^:skip-aot cbord-api.core
  :target-path "target/%s"
  :uberjar-name "cbord-api.jar"
  :profiles {:uberjar {:aot :all}})
