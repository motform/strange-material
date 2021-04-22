(ns motform.strange.lkml
  (:require [clojure.string       :as str]
            [clojure.java.shell   :as shell]))

(defn git-revisions [git-path]
  (->> (shell/sh "git" "-C" git-path "rev-list" "--pretty=oneline" "--reverse" "master")
       :out
       str/split-lines
       (pmap (fn [s] [(subs s 0 40) (subs s 41)])))) ; not sure we need both?

(defn sha->pi-email [sha git-path]
  (shell/sh "git" "-C" git-path "show" (str sha ":m")))



(comment 

  (def test-path "/Users/lla/Projects/github/lkml/lkml/git/0.git")
  (def r (git-revisions test-path))

  (def ms (->> r
               (take 1000)
               (map (comp parse-email
                          :out
                          #(sha->pi-email % test-path)
                          first))))

  (nth r 1000)
  (-> ms last)


  

  (defn email-header? [line]
    (re-find #"^.+: .+$" line))

  (defn parse-email-header [parsed-email email-header]
    (let [[k v] (str/split email-header #": ")]
      (assoc parsed-email (-> k str/lower-case keyword) v)))

  (defn parse-email [email]
    (loop [parsed-email {}
           lines        (str/split-lines email)]
      (if (email-header? (first lines))
        (recur (parse-email-header parsed-email (first lines)) (rest lines))
        (assoc parsed-email :body lines))))

  
  )
