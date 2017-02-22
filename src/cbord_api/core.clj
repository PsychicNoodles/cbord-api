(ns cbord-api.core
  (:gen-class)
  (:require [org.httpkit.server :refer [run-server]]
            [cbord-api.routes :as routes]
            [environ.core :refer [env]]))

(defn -main
  [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (run-server routes/cbord-api-routes {:port port :join? false})))
