(ns cbord-api.api
  (:gen-class)
  (:require [cbord-api.pdf :as pdf]
            [clj-http.client :as client]
            [net.cgrand.enlive-html :as html]
            [clojure.string :as string]
            [clojure.java.io :refer [as-url]]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [slingshot.slingshot :refer [throw+]]))

(def institution "grinnell")

(def base-url (format "https://get.cbord.com/%s/full/" institution))

(def transaction-date-format
  "The format required for dates when getting a transactions pdf"
  (f/formatter "yyyy-MM-dd"))

(defn- transaction-start-date
  "Today's date minus 3 months, which should encapsulate a full semester.
   CBORD seems to return an error PDF when the difference in days reaches 209 for some reason."
  []
  (f/unparse-local transaction-date-format (t/minus (t/today) (t/months 3))))

(def transaction-pdf-url-fmt
  "The URL for requesting a transactions pdf"
  (str base-url "historyPDF.php?dateS=%s&dateE=%s"))

(defn- check-timed-out
  [res]
  (if
    (and (= (:status res) 302)
         (= (get (:headers res) "Location") "login.php"))
    (throw+ {:type :timeout :res res})
    res))

(defn- html-first-val
  [res selector]
  (-> res
      check-timed-out
      :body
      html/html-snippet
      (html/select selector)
      first
      :attrs
      :value))

(defn- get-form-token
  [cs]
  (html-first-val (client/get (str base-url "login.php")
                              {:cookie-store cs})
                  [[:input (html/attr= :name "formToken")]]))


(defn- send-login
  "Returns true when successful and false otherwise"
  [cs token username password]
  (let [res (client/post (str base-url "login.php")
                         {:form-params {:formToken token
                                        :username username
                                        :password password
                                        :submit "Login"}
                          :cookie-store cs})]
    (check-timed-out res)
    (and
      (= (:status res) 302)
      (= (-> res
             (:headers)
             (get "Location"))
         "index.php"))))

(defn login
  [username password]
  (let [cs (clj-http.cookies/cookie-store)
        form-token (get-form-token cs)]
    (if (send-login cs form-token username password)
      cs)))

(defn- get-balances-token
  [cs]
  (let [res (client/get (str base-url "funds_home.php")
                        {:cookie-store cs})]
    (check-timed-out res)
    {:user-id (re-find #"(?<=getOverview\(\")[a-zA-Z0-9-]+" (:body res))
     :form-token (html-first-val res
                                 [[:#address_popup_select_address_form]
                                  [:input (html/attr= :name "formToken")]])}))

(defn get-balances
  ([cs]
   (let [{:keys [user-id form-token]} (get-balances-token cs)]
    (get-balances cs user-id form-token)))
  ([cs user-id form-token]
   (zipmap [:meals :campus :dining :guest]
           (drop 1
             (map (comp str first :content)
               (->
                (client/post (str base-url "funds_overview_partial.php")
                             {:form-params {:userId user-id
                                            :formToken form-token}
                              :cookie-store cs})
                check-timed-out
                :body
                html/html-snippet
                (html/select [:.balance])))))))

(defn get-transactions
  [cs & {:keys [start end flat]
         :or {start (transaction-start-date)
              end (f/unparse-local transaction-date-format (t/today))
              flat false}}]
  (let [res (check-timed-out (client/get
                               (format transaction-pdf-url-fmt start end)
                               {:cookie-store cs :as :stream}))
        trans (pdf/extract-pdf (:body res))]
      (if flat
        (flatten trans)
        trans)))
