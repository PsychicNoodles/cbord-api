(ns cbord-api.core
  (:gen-class)
  (:require [org.httpkit.server :refer [run-server]]
            [cbord-api.routes :as routes]))

(defn -main
  [& args]
  (run-server routes/cbord-api-routes {:port 8080}))
