(ns cbord-api.routes
  (:require [cbord-api.api :as api]
            [compojure.route :refer [not-found]]
            [compojure.core :refer [defroutes POST GET]]
            [ring.logger :refer [wrap-with-logger]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.middleware.params :refer [wrap-params]]
            [clojure.tools.logging :refer [info error]]
            [slingshot.slingshot :refer [try+]]))

(def debug false)

(defn- res
  "Creates a Ring response map with the parameter status and body"
  [status body]
  {:status status :body body :headers (hash-map)})

(defn- is-error
  "Checks if the status of the Ring response map is in the 400s"
  [r]
  (and
    (> (:status r) 399)
    (< (:status r) 500)))

(defn- wrap-logging
  "Returns a function that takes a Ring request map and calls the parameter
  function. It also info logs the parameter name and remote address of the
  incoming Ring request when it starts and succeeds or fails."
  [name f]
  (fn [req]
    (let [addr (:remote-addr req)]
      (info (str "start of " name " for " addr))
      (let [r (f req)]
        (if (is-error r)
          (info (str "failed " name " for " addr))
          (info (str "succeeded " name " for " addr)))
        r))))


(def ^{:private true} login-cookies (atom {}))

(def
  ^{:private true
    :doc "Attempts to login a user, storing the cookies (including, most
          importantly, session) in the `login-cookies` atom."}
  login-handler
  (wrap-logging
    "login"
    (fn [{{:keys [username password]} :params}]
      (if-let [cs (api/login username password)]
        (do
          (swap! login-cookies assoc username cs)
          (res 200 {:status "ok"}))
        (do
          (res 401 {:status "login failed"}))))))

(def
  ^{:private true
    :doc "Attempts to load the user's balances using the cookies associated with
          the parameter username"}
  balances-handler
  (wrap-logging
    "balances"
    (fn [{{:keys [username]} :params}]
      (if-let [cs (get @login-cookies username)]
        (res 200 (assoc (api/get-balances cs) :status "ok"))
        (res 401 {:status "not authorized/logged in"})))))

(def
  ^{:private true
    :doc "Attempts to load the user's transactions using the cookies associated
          with the parameter username. Can also limit the number of items
          returned and change the start and end date ranges"}
  transactions-handler
  (wrap-logging
    "transactions"
    (fn [{{:keys [username limit start end flatten]} :params}]
      (if-let [cs (get @login-cookies username)]
        (res 200 (api/get-transactions cs :limit limit :start start
                                          :end end :flatten flatten))
        (res 401 {:status "not authorized/logged in"})))))

(def
  ^{:private true
    :doc "Attempts to login the user (if not already) and load balances"}
  all-handler
  (wrap-logging
    "all"
    (fn [req]
      (let [login-res (login-handler req)]
        (info (str "login-res: " login-res))
        (if (is-error login-res)
          login-res
          (balances-handler req))))))

(defn wrap-timeout-handling
  "Returns a handler that catches a timeout exception from the handler. If such
  an exception is caught, it error logs, resets stored login info, and recalls
  the handler if the username and password parameters were provided."
  [handler]
  (fn [req]
    (try+
      (handler req)
      (catch [:type :timeout] {:keys [res]}
        (error (str "Timeout during" (:request-method req) " req to " (:uri req)))
        (error res)
        (if (and
              (contains? (:params req) :username)
              (contains? (:params req) :password))
            (do
              (login-handler req)
              (handler req))
            (res 401 {:status "login timed out"}))))))

(defn- not-found-handler
  []
  (res 404 {:status "not found"}))

(defroutes ^{:private true} all-routes
  (POST "/login" req (login-handler req))
  (GET "/balances" req (balances-handler req))
  (GET "/transactions" req (transactions-handler req))
  (POST "/all" req (all-handler req))
  (POST "/debug" req (if debug (res 200 (str req)) (not-found-handler)))
  (not-found (not-found-handler)))


(def cbord-api-routes
  (->
    all-routes
    wrap-params
    wrap-timeout-handling
    wrap-json-response
    wrap-with-logger
    (wrap-defaults api-defaults)))
