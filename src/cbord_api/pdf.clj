(ns cbord-api.pdf
  (:require [clj-http.client :as client]
            [clojure.tools.logging :refer [info error]]
            [clojure.java.io :as io]
            [slingshot.slingshot :refer [try+ throw+]])
  (:import [org.apache.pdfbox.pdmodel PDDocument]
           [org.apache.pdfbox.text PDFTextStripperByArea]
           [org.apache.pdfbox.io MemoryUsageSetting]
           [java.awt Rectangle]
           [java.io IOException]))

(def regions-left "The left coord of the regions" 28)

(def regions-top "The top coord of the highest region" 139)

(def regions-width "The width of the regions" 539)

(def regions-height "The height of the regions" 17)

(def regions-count "The max number of regions" 36)

(def max-retries
  "The number of retries to resolve Scratch file or COSStream errors"
  3)

(defn- extract-page
  [page stripper]
  (.extractRegions stripper page)
  (map #(.getTextForRegion stripper (str %)) (range regions-count)))

(defn- extract-all
  [input stripper]
  (with-open [pdf (PDDocument/load input)]
    (doall (map #(extract-page % stripper) (.getPages pdf)))))

(defn extract-pdf
  [input]
  (try+
   (let [stripper (PDFTextStripperByArea.)]
     (doseq [n (range regions-count)]
       (.addRegion stripper
                   (str n)
                   (Rectangle. regions-left
                               (+ regions-top (* regions-height n))
                               regions-width
                               regions-height)))
     (extract-all input stripper))))
