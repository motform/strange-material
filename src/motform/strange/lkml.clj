(ns motform.strange.lkml
  (:require [clojure.string       :as str]
            [clojure.java.io      :as io]
            [clojure-mail.message :as email]
            [clojure.java.shell   :as shell]
            [next.jdbc.sql        :as sql]
            [next.jdbc            :as jdbc])
  (:import  [java.io             ByteArrayInputStream]
            [javax.mail          Session]
            [javax.mail.internet MimeMessage]
            [java.util           Properties]))

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
   (let [props (Session/getDefaultInstance (Properties.))]
     (with-open [msg (->stream email-str)]
       (MimeMessage. props msg)))))

(defn- topmost-path [path]
  (last (str/split path #"/")))

(defn lkml-paths [top-path]
  (let [paths (transient [])]
    (doseq [list-path (.listFiles (io/file top-path))]
      (let [list-name (-> list-path .getAbsolutePath topmost-path)]
        (doseq [list-container-path (.listFiles (io/file list-path))]
          (doseq [sublist-path (.listFiles (io/file list-container-path))]
            (when (.isDirectory sublist-path)
              (conj! paths [list-name (.getAbsolutePath sublist-path)]))))))
    (persistent! paths)))

(defn parse-emails [[list-name git-path]]
  (->> git-path
       git-revisions
       (pmap (comp #(assoc % :list list-name)
                   parse-email
                   (partial sha->pi-email git-path)))))



(defn email->query [email]
  {:list    (-> email :list)
   :author  (-> email :from first :name)
   :address (-> email :from first :address)
   :date    (-> email :date-sent)
   :subject (-> email :subject)
   :body    (-> email :body :body)})

(comment 

  (def db {:dbtype "sqlite" :dbname "resources/db/emails"}) ; the old DB
  (def db {:dbtype "sqlite" :dbname "resources/db/lkml"})
  (def ds (jdbc/get-datasource db))
  (jdbc/execute! ds ["CREATE TABLE emails(list CHAR(255), author CHAR(255), address CHAR(255), date CHAR(255), subject TEXT, body TEXT);"])
  ;; (def ds (jdbc/get-datasource "jdbc:sqlite:/Users/lla/Projects/strange_material/resources/db/lkml"))


  ;; OLD - 2, 8

  ;; [0 1]
  (def emails
    (->> (lkml-paths "/Users/lla/Projects/github/lkml/")
         (filter (fn [[list _]] (= list "lkml")))
         (into [])
         (sort)
         #_(drop 2)
         (take 2)
         (pmap (comp (partial pmap email->query)
                     parse-emails))))

  (doseq [list emails]
    (doseq [query list]
      (sql/insert! ds :emails query)))

  (def email-table
    "CREATE TABLE emails(list CHAR(255), author CHAR(255), address CHAR(255), date CHAR(255), subject TEXT, body TEXT);")

  (count (jdbc/execute! ds ["select * from emails"]))

  )
