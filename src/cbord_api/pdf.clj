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

(def acct-col-width "The width of the accounts column" 128)

(def dt-col-width "The width of the date & time column" 156)

(def act-col-width "The width of the activity column" 156)

(def amt-col-width "The width of the amount column" 99)

(def regions-height "The height of the regions" 17)

; (def regions-rect (Rectangle. regions-left regions-top regions-width regions-height))

(def acct-col-rect (Rectangle. regions-left
                               regions-top
                               acct-col-width
                               regions-height))

(def dt-col-rect (Rectangle. (+ (.getX acct-col-rect) acct-col-width)
                             regions-top
                             dt-col-width
                             regions-height))

(def act-col-rect (Rectangle. (+ (.getX dt-col-rect) dt-col-width)
                              regions-top
                              act-col-width
                              regions-height))

(def amt-col-rect (Rectangle. (+ (.getX act-col-rect) act-col-width)
                              regions-top
                              amt-col-width
                              regions-height))

(def regions-count "The max number of regions" 36)

(def max-retries
  "The number of retries to resolve Scratch file or COSStream errors"
  3)

(defn- setup-regions
  [stripper base-rect name]
  (dotimes [n regions-count]
    (let [copy (.clone base-rect)]
      (.translate copy 0 (* regions-height n))
      (.addRegion stripper (str name n) copy))))

(defn- extract-page
  [page stripper name]
  (.extractRegions stripper page)
  (map #(.getTextForRegion stripper (str name %)) (range regions-count)))

(defn- extract-all
  [pdf stripper name]
  (doall (map #(extract-page % stripper name) (.getPages pdf))))

(defn- merge-in-page
  [acct dt act amt]
  (remove #(= (:acct %) "\n")
    (map #(hash-map :acct %1 :dt %2 :act %3 :amt %4) acct dt act amt)))

(defn extract-pdf
  [input]
  (try+
   (let [stripper (PDFTextStripperByArea.)]
     (setup-regions stripper acct-col-rect "acct")
     (setup-regions stripper dt-col-rect "dt")
     (setup-regions stripper act-col-rect "act")
     (setup-regions stripper amt-col-rect "amt")
     (with-open [pdf (PDDocument/load input)]
       (map
         merge-in-page
         (extract-all pdf stripper "acct")
         (extract-all pdf stripper "dt")
         (extract-all pdf stripper "act")
         (extract-all pdf stripper "amt"))))))
