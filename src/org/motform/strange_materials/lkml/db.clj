(ns org.motform.strange-materials.lkml.db
  (:require [clojure.string       :as str]
            [clojure.java.io      :as io]
            [clojure-mail.message :as email]
            [clojure.java.shell   :as shell]
            [next.jdbc.sql        :as sql]
            [next.jdbc            :as jdbc])
  (:import  [java.io    ByteArrayInputStream]
            [java.util  Properties]
            [javax.mail Session]
            [javax.mail.internet MimeMessage]))

;; PUBLIC-INBOX WRANGLING

(defn- git-revisions [git-path]
  (->> (shell/sh "git" "-C" git-path "rev-list" "--pretty=oneline" "--reverse" "master")
       :out
       str/split-lines
       (pmap #(subs % 0 40)))) ; get the SHA

(defn- sha->pi-email [git-path sha]
  (:out (shell/sh "git" "-C" git-path "show" (str sha ":m"))))

(defn- ->stream [s]
  (-> s (.getBytes "UTF-8") (ByteArrayInputStream.)))

(defn- parse-email [email-str]
  (email/read-message
   (with-open [msg (->stream email-str)]
     (MimeMessage. (Session/getDefaultInstance (Properties.)) msg))))

(defn- topmost-path [path]
  (last (str/split path #"/")))

(defn- lkml-paths [top-path]
  (let [paths (transient [])]
    (doseq [list-path (.listFiles (io/file top-path))]
      (let [list-name (-> list-path .getAbsolutePath topmost-path)]
        (doseq [list-container-path (.listFiles (io/file list-path))]
          (doseq [sublist-path (.listFiles (io/file list-container-path))]
            (when (.isDirectory sublist-path)
              (conj! paths [list-name (.getAbsolutePath sublist-path)]))))))
    (persistent! paths)))

(defn- parse-emails [[list-name git-path]]
  (->> git-path
       git-revisions
       (pmap (comp #(assoc % :list list-name)
                   parse-email
                   (partial sha->pi-email git-path)))))

(defn- email->query [email]
  {:author  (-> email :from first :name)
   :address (-> email :from first :address)
   :date    (-> email :date-sent)
   :subject (-> email :subject)
   :body    (-> email :body :body)})

;; DATABASE QUERY

(def ^:private db {:dbtype "sqlite" :dbname "resources/db/lkml-fts"})
(def ^:private ds (jdbc/get-datasource db))

(defn- emails-by-key [key]
  (fn [s limit offset]
    (jdbc/execute! ds [(format "SELECT rowid, * FROM email
                                WHERE %s MATCH '%s'
                                ORDER BY date
                                LIMIT %d
                                OFFSET %d"
                               key s limit offset)])))

(def emails-by-subject (emails-by-key "subject"))
(def emails-by-body    (emails-by-key "body"))

(defn- count-emails-by-key [key]
  (fn [s]
    (if s
      (let [q (format "SELECT count(*) FROM email WHERE %s MATCH '%s'" key s)]
        (-> (jdbc/execute! ds [q]) first vals first))
      "no")))

(def count-emails-by-subject (count-emails-by-key "subject"))
(def count-emails-by-body    (count-emails-by-key "body"))

(comment 
  (count-emails-by-subject "open")

  (def emails ;; [0 1 2]
    (->> (lkml-paths "/Users/lla/Projects/github/lkml/")
         (filter (fn [[list _]] (= list "lkml")))
         (sort)
         (drop 3)
         (take 1)
         (pmap (comp (partial pmap email->query)
                     parse-emails))))

  (doseq [list emails]
    (doseq [query list]
      (sql/insert! ds :email query)))

  (sql/query ds ["SELECT author FROM email WHERE rowid = 10000"])
  ;; (jdbc/execute! ds ["CREATE VIRTUAL TABLE email USING fts5(author, address, date, subject, body)"])
  )
