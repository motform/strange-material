(ns motform.strange.material.kernel
  (:require [cljfx.api                       :as fx]      
            [clojure.data.csv                :as csv]     
            [clojure.string                  :as str]     
            [motform.strange.util            :as util]
            [motform.strange.material.events :as events]  
            [motform.strange.material.views  :as views]))

(defn- ->kernel-archive-header [s]
  (if (str/blank? s) :id
      (-> s str/lower-case keyword)))

(def kernel-email-archive
  (let [emails (-> "resources/csv/kernel_archive.csv" slurp csv/read-csv)
        header (->> emails first (map ->kernel-archive-header))]
    (pmap #(zipmap header %) (rest emails))))

(def strace-out
  (->> "resources/txt/strace_ls.txt"
       slurp
       str/split-lines
       drop-last
       (map (fn [ps] #:process
              {:call ps
               :name (re-find #"^\w*" ps)}))))

;; (defn emails-by-key [key]
;;   (fn [str]
;;     (remove nil? (filter #(when (string? (get % key))
;;                             (str/includes? (get % key) str)) kernel-email-archive))))

(defn emails-by-key [key]
  (fn [str]
    (remove nil? (take 246 kernel-email-archive))))

(def emails-by-topic (emails-by-key :topic))
(def emails-by-msg   (emails-by-key :msg))

;;; EVENTS

(defmethod events/event-handler ::select-process [{:keys [fx/context process]}]
  {:context (fx/swap-context context assoc :kernel/selected process)})

(defmethod events/event-handler ::select-email [{:keys [fx/context email]}]
  {:context (fx/swap-context context assoc :kernel/email email)})

;;; SUBS

(defn selected-process [context]
  (fx/sub-val context :kernel/selected))

(defn selected-email [context]
  (fx/sub-val context :kernel/email))

;;; VIEWS

(defn process-item [{:keys [process selected?]}]
  {:fx/type          :h-box
   :style-class      ["kernel-process-item" (when selected? "kernel-process-item-selected")]
   :on-mouse-clicked {:event/type ::select-process
                      :process    process}
   :children [{:fx/type :label
               :text    (process :process/name "")}]})

(defn process-view [{:fx/keys [context]}]
  (let [selected-process (fx/sub-ctx context selected-process)
        processes        (util/distinct-by-key strace-out :process/name)]
    {:fx/type      :v-box
     :style-class  "kernel-process-view"
     :spacing      3
     :children     (for [process processes]
                     {:fx/type   process-item
                      :process   process
                      :selected? (= process selected-process)})}))

(defn call-stack [{:fx/keys [context]}]
  {:fx/type      :scroll-pane
   :min-width    150
   :style-class  "kernel-call-stack"
   :fit-to-width true
   :content      {:fx/type :v-box
                  :spacing 15
                  :children [{:fx/type :label
                              :text    "ls system calls"}
                             {:fx/type process-view}]}})

(defn email-empty [_]
  {:fx/type :label
   :text    "Nobody seemed all to interested about this."})

(defn email-expanded [{:keys [email]}]
  {:fx/type :v-box
   :spacing 10
   :children [{:fx/type     :label
               :style-class "kernel-email-view-meta"
               :text        (str/upper-case (str (:author email) "  —  " (:time email)))}
              {:fx/type     :text-area
               :style-class "kernel-email-view-msg"
               :text        (:msg email)
               :min-height  400}]})

(defn email-view [{:keys [email selected?]}]
  {:fx/type          :v-box
   :style-class      ["kernel-email-view" (when selected? "kernel-email-view-selected")]
   :on-mouse-clicked {:event/type ::select-email
                      :email     (:id email)}
   :children         [{:fx/type     :label
                       :style-class (if selected? "kernel-email-view-topic" "")
                       :text        (:topic email)}
                      (if selected?
                        {:fx/type email-expanded
                         :email   email}
                        {:fx/type :v-box})]})

(defn email-list [{:fx/keys [context]}]
  (let [selected-email (fx/sub-ctx context selected-email)
        process        (fx/sub-ctx context selected-process)
        emails         (emails-by-topic (:process/name process))]
    {:fx/type  :v-box
     :children (if (empty? emails)
                 [{:fx/type email-empty}]
                 (for [email emails]
                   {:fx/type    email-view
                    :email      email
                    :selected?  (= (:id email) selected-email)}))}))

(defn process-emails [{:fx/keys [context]}]
  (let [process     (fx/sub-ctx context selected-process)
        email-count (count (emails-by-topic (:process/name process)))]
    {:fx/type       :scroll-pane
     :style-class   ["kernel-call-stack"]
     :fit-to-width  true
     :fit-to-height true
     :content       {:fx/type   :v-box
                     :spacing   20
                     :children  [{:fx/type :label
                                  :text    (if (str/blank? (str (:process/name process)))
                                             "No process selected."
                                             (str (:process/name process) " (" email-count ")"))}
                                 {:fx/type email-list}]}}))

;; (defmethod views/panel :panel/kernel [_]
;;   {:fx/type     :v-box
;;    :style-class ["panel"]
;;    :children    [{:fx/type  :h-box
;;                   :spacing  20
;;                   :children [{:fx/type call-stack}
;;                              {:fx/type process-emails}]}]})

(defmethod views/panel :panel/kernel [_]
  {:fx/type     :v-box
   :style-class ["panel"]
   :children    [{:fx/type :border-pane
                  :left    {:fx/type call-stack}
                  :center  {:fx/type process-emails}}]})

