(ns org.motform.strange-materials.lkml.db
  "DB query functions and scripts that build a database from a public-inbox/LKML-git tree.
  Use the forms in the rich comment at the bottom of the file to populate the database."
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

;;;; PUBLIC-INBOX WRANGLING

(defn- git-revisions
  "Get commit SHAs from a repository at `git-path`."
  [git-path]
  (->> (shell/sh "git" "-C" git-path "rev-list" "--pretty=oneline" "--reverse" "master")
       :out
       str/split-lines
       (pmap #(subs % 0 40)))) ; get the SHA

(defn- sha->pi-email
  "Get the public-inbox email in `sha` at `git-path`."
  [git-path sha]
  (:out (shell/sh "git" "-C" git-path "show" (str sha ":m"))))

(defn- ->stream [s]
  (-> s (.getBytes "UTF-8") (ByteArrayInputStream.)))

(defn- parse-email [email-str]
  (email/read-message
   (with-open [msg (->stream email-str)]
     (MimeMessage. (Session/getDefaultInstance (Properties.)) msg))))

(defn- topmost-path [path]
  (last (str/split path #"/")))

(defn- lkml-paths
  "Return a vector of LKML list tuples, in the format [list-name path].
  Expects `top-path` to be the root of the grokmirror directory."
  [top-path]
  (let [paths (transient [])]
    (doseq [list-path (.listFiles (io/file top-path))]
      (let [list-name (-> list-path .getAbsolutePath topmost-path)]
        (doseq [list-container-path (.listFiles (io/file list-path))]
          (doseq [sublist-path (.listFiles (io/file list-container-path))]
            (when (.isDirectory sublist-path)
              (conj! paths [list-name (.getAbsolutePath sublist-path)]))))))
    (persistent! paths)))

(defn- parse-emails
  "Return publix-inbox emails in map format suitable for `email->query`.
  Expects its argument to be a tuple from `lkml-paths`."
  [[list-name git-path]]
  (->> git-path
       git-revisions
       (pmap (comp #(assoc % :list list-name)
                   parse-email
                   (partial sha->pi-email git-path)))))

(defn- email->query
  "Build a next.jdbc-compatible insertion query from `email`."
  [email]
  {:author  (-> email :from first :name)
   :address (-> email :from first :address)
   :date    (-> email :date-sent)
   :subject (-> email :subject)
   :body    (-> email :body :body)})

(defn ready-emails
  "Ready emails for insertion into `db`.
  Excepts `list` to be the name `lkml-paths`-name of the desired list."
  [list]
  (->> (lkml-paths "/Users/lla/Projects/github/lkml/")
       (filter (fn [[list-name _]]
                 (= list-name list)))
       (pmap (comp (partial pmap email->query)
                   parse-emails))))


;;;; DATABASE QUERY

(defonce ^:private db {:dbtype "sqlite" :dbname "resources/db/lkml-fts"})
(defonce ^:private ds (jdbc/get-datasource db))

(defn- emails-by-key
  "Factory function for column/key query."
  [key]
  (fn [s limit offset]
    (jdbc/execute! ds [(format "SELECT rowid, * FROM email
                                WHERE %s MATCH '%s'
                                ORDER BY date
                                LIMIT %d
                                OFFSET %d"
                               key s limit offset)])))

(def emails-by-subject (emails-by-key "subject"))
(def emails-by-body    (emails-by-key "body"))

(defn- count-emails-by-key
  "Factory function for column/key count."
  [key]
  (fn [s]
    (if s
      (let [q (format "SELECT count(*) FROM email WHERE %s MATCH '%s'" key s)]
        (-> (jdbc/execute! ds [q]) first vals first))
      "no")))

(def count-emails-by-subject (count-emails-by-key "subject"))
(def count-emails-by-body    (count-emails-by-key "body"))

(comment 
  ;; table "schema"
  (jdbc/execute! ds ["CREATE VIRTUAL TABLE email USING fts5(author, address, date, subject, body)"])

  ;; insert the readied emails into the database
  (doseq [list (ready-emails "$LIST$")]
    (doseq [query list]
      (sql/insert! ds :email query)))
  )
