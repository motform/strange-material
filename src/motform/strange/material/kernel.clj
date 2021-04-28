(ns motform.strange.material.kernel
  (:require [cljfx.api                       :as fx]      
            [clojure.string                  :as str]     
            [motform.strange.lkml            :as lkml]
            [motform.strange.util            :as util]
            [motform.strange.material.events :as events]  
            [motform.strange.material.views  :as views]))

;;; COMMAND LINE

(defmethod events/event-handler ::change-command [{:fx/keys [context event]}]
  {:context (fx/swap-context context assoc :kernel/command-line event)})

(defn command-line-command [context]
  (fx/sub-val context :kernel/command-line))

(defn command-line [{:fx/keys [context]}]
  (let [command (fx/sub-ctx context command-line-command)]
    {:fx/type     :v-box
     :style-class ["kernel-command-line"]
     :children [{:fx/type         :text-field
                 :on-text-changed {:event/type ::change-command}
                 :text            command}]}))

;;; SYSTEM CALLS

(def strace-out
  (->> "resources/txt/strace_ls.txt"
       slurp
       str/split-lines
       drop-last
       (map (fn [ps] #:process
              {:call ps
               :name (re-find #"^\w*" ps)}))))

(defmethod events/event-handler ::select-system-call [{:keys [fx/context system-call]}]
  {:context (fx/swap-context context assoc :kernel/selected-system-call system-call)})

(defn selected-system-call [context]
  (fx/sub-val context :kernel/selected-system-call))

(defn system-call-item [{:keys [system-call selected?]}]
  {:fx/type          :h-box
   :style-class      ["kernel-system-call-item" (when selected? "kernel-system-call-item-selected")]
   :on-mouse-clicked {:event/type  ::select-system-call
                      :system-call system-call}
   :children [{:fx/type :label
               :text    (system-call :process/name "")}]})

(defn system-call-view [{:fx/keys [context]}]
  (let [selected-system-call (fx/sub-ctx context selected-system-call)
        system-calls         (util/distinct-by-key strace-out :process/name)]
    {:fx/type      :v-box
     :style-class  "kernel-system-call-view"
     :spacing      3
     :children     (for [system-call system-calls]
                     {:fx/type     system-call-item
                      :system-call system-call
                      :selected?   (= system-call selected-system-call)})}))

(defn call-stack [{:fx/keys [context]}]
  {:fx/type      :scroll-pane
   :min-width    150
   :style-class  "kernel-call-stack"
   :fit-to-width true
   :content      {:fx/type :v-box
                  :spacing 15
                  :children [{:fx/type :label
                              :text    "ls system calls"}
                             {:fx/type system-call-view}]}})

;;; EMAILS

(defmethod events/event-handler ::select-email [{:keys [fx/context email]}]
  {:context (fx/swap-context context assoc :kernel/email email)})

(defn selected-email [context]
  (fx/sub-val context :kernel/email))

(defn email-empty [_]
  {:fx/type :label
   :text    "Nobody seemed all to interested about this."})

(defn email-expanded [{{:email/keys [author body]} :email}]
  {:fx/type :v-box
   :spacing 10
   :children [{:fx/type     :label
               :style-class "kernel-email-view-meta"
               :text        (str/upper-case author)} ; TODO add date
              {:fx/type     :text-area
               :style-class "kernel-email-view-body"
               :text        body
               :min-height  400}]})

(defn email-view [{:keys [selected?] {:email/keys [subject rowid] :as email} :email}]
  {:fx/type          :v-box
   :style-class      ["kernel-email-view" (when selected? "kernel-email-view-selected")]
   :on-mouse-clicked {:event/type ::select-email
                      :email      rowid}
   :children         [{:fx/type     :label
                       :style-class (if selected? "kernel-email-view-topic" "")
                       :text        subject}
                      (if selected?
                        {:fx/type email-expanded
                         :email   email}
                        {:fx/type :v-box})]})

(defn email-list [{:fx/keys [context]}]
  (let [selected-email (fx/sub-ctx context selected-email)
        system-call    (fx/sub-ctx context selected-system-call)
        emails         (lkml/emails-by-subject (:process/name system-call) 20 0)]
    {:fx/type  :v-box
     :children (if (empty? emails)
                 [{:fx/type email-empty}]
                 (for [email emails]
                   {:fx/type    email-view
                    :email      email
                    :selected?  (= (:email/rowid email) selected-email)}))}))

(defn system-call-emails [{:fx/keys [context]}]
  (let [system-call (fx/sub-ctx context selected-system-call)
        ;; TODO  email-count (count (emails-by-topic (:process/name process))) 
        ]
    {:fx/type       :scroll-pane
     :style-class   ["kernel-call-stack"]
     :fit-to-width  true
     :fit-to-height true
     :content       {:fx/type   :v-box
                     :spacing   20
                     :children  [{:fx/type :label
                                  :text    (if (str/blank? (str (:process/name system-call)))
                                             "No system call selected."
                                             ;; TODO (str (:process/name process) " (" email-count ")") ;; TODO
                                             (:process/name system-call)
                                             )}
                                 {:fx/type email-list}]}}))

(defmethod views/panel :panel/kernel [_]
  {:fx/type       :v-box
   :style-class   ["panel"]
   :children      [{:fx/type :border-pane
                    :top     {:fx/type command-line}
                    :left    {:fx/type call-stack}
                    :center  {:fx/type system-call-emails}}]})
