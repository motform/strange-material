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

;;; SIDEBAR

(def strace-out
  (->> "resources/txt/strace_ls.txt"
       slurp
       str/split-lines
       drop-last
       (map (fn [ps] #:process
              {:call ps
               :name (re-find #"^\w*" ps)}))))

(def std-out-out (str/split-lines "Applications     Desktop    Downloads  Mail    Music     Projects  References\nCalibre Library  Documents  Library    Movies  Pictures  Public    VirtualBox VMs"))

(defmethod events/event-handler ::select-system-call [{:keys [fx/context system-call]}]
  {:context (fx/swap-context context assoc :kernel/selected-system-call system-call)})

(defn sidebar-container-label [{:keys [label]}]
  {:fx/type     :v-box
   :style-class "kernel-sidebar-container-label"
   :children    [{:fx/type :label
                  :text    (str/upper-case label)}]})

(defn selected-system-call [context]
  (fx/sub-val context :kernel/selected-system-call))

(defn system-call-item [{:keys [system-call selected?]}]
  {:fx/type          :h-box
   :style-class      (if selected? "kernel-system-call-item-selected" "kernel-system-call-item")
   :on-mouse-clicked {:event/type  ::select-system-call
                      :system-call (:process/name system-call)}
   :children [{:fx/type :label
               :text    (system-call :process/name "")}]})

(defn system-call-view [{:fx/keys [context]}]
  (let [selected-system-call (fx/sub-ctx context selected-system-call)
        system-calls         (util/distinct-by-key strace-out :process/name)]
    {:fx/type     :v-box
     :style-class "kernel-sidebar-container"
     :children    [{:fx/type sidebar-container-label
                    :label   "system calls"}
                   {:fx/type      :v-box
                    :style-class  "kernel-sidebar-list"
                    :spacing      3
                    :children     (for [system-call system-calls]
                                    {:fx/type     system-call-item
                                     :system-call system-call
                                     :selected?   (= (:process/name system-call) selected-system-call)})}]}))

(defn std-out-item [{:keys [line]}]
  {:fx/type     :h-box
   :style-class ["kernel-std-out-item"]
   :children [{:fx/type :label
               :text    line}]})

(defn std-out-view [_]
  {:fx/type     :v-box
   :style-class "kernel-sidebar-container"
   :children    [{:fx/type sidebar-container-label
                  :label   "output"}
                 {:fx/type      :v-box
                  :style-class  "kernel-sidebar-list"
                  :spacing      3
                  :children     (for [line std-out-out]
                                  {:fx/type std-out-item
                                   :line    line})}]})

(defn stack-trace-item [{:keys [system-call selected?]}]
  {:fx/type          :h-box
   :style-class      (if selected? "kernel-system-call-item-selected" "kernel-system-call-item")
   :on-mouse-clicked {:event/type  ::select-system-call
                      :system-call (:process/name system-call)}
   :children [{:fx/type :label
               :text    (system-call :process/call "")}]})

(defn stack-trace-view [{:fx/keys [context]}]
  (let [selected-system-call (fx/sub-ctx context selected-system-call)]
    {:fx/type     :v-box
     :style-class "kernel-sidebar-container"
     :children    [{:fx/type sidebar-container-label
                    :label   "stack trace"}
                   {:fx/type      :v-box
                    :style-class  "kernel-sidebar-list"
                    :spacing      3
                    :children     (for [system-call strace-out]
                                    {:fx/type     stack-trace-item
                                     :system-call system-call
                                     :selected?   (= (:process/name system-call) selected-system-call)})}]}))

(defn sidebar [_]
  {:fx/type      :scroll-pane
   :max-width     (/ (util/window-width) 3)
   :style-class  "kernel-sidebar"
   :fit-to-width true
   :content      {:fx/type :v-box
                  :children [{:fx/type system-call-view}
                             {:fx/type std-out-view}
                             {:fx/type stack-trace-view}]}})

;;; EMAILS

(defn email-empty [_]
  {:fx/type :label
   :text    ""})

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

(defmethod events/event-handler ::select-email [{:keys [fx/context email]}]
  {:context (fx/swap-context context assoc :kernel.email/selected email :kernel.email/offset 50)})

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

(defn selected-email [context]
  (fx/sub-val context :kernel.email/selected))

(defn email-offset [context]
  (fx/sub-val context :kernel.email/offset))

(defn email-list [{:fx/keys [context]}]
  (let [selected-email (fx/sub-ctx context selected-email)
        system-call    (fx/sub-ctx context selected-system-call)
        offset         (fx/sub-ctx context email-offset)
        emails         (if system-call (lkml/emails-by-subject system-call 100 offset) [])]
    {:fx/type  :v-box
     :children (if (empty? emails)
                 [{:fx/type email-empty}]
                 (for [email emails]
                   {:fx/type    email-view
                    :email      email
                    :selected?  (= (:email/rowid email) selected-email)}))}))

(defmethod events/event-handler ::increment-email-offset [{:fx/keys [context]}]
  {:context (fx/swap-context context update :kernel.email/offset + 20)})

(defn email-offset-button [{:fx/keys [context]}]
  {:fx/type          :label
   :style-class      "kernel-email-offset-button"
   :on-mouse-clicked {:event/type  ::increment-email-offset}
   :text             (str/upper-case "more emails")})

(defn system-call-emails [{:fx/keys [context]}]
  (let [system-call (fx/sub-ctx context selected-system-call)
        email-count (lkml/count-emails-by-subject system-call)]
    {:fx/type       :scroll-pane
     :style-class   "kernel-emails"
     :min-width     (* 2 (/ (util/window-width) 3))
     :fit-to-width  true
     :fit-to-height true
     :content       {:fx/type   :v-box
                     :children  [{:fx/type sidebar-container-label
                                  :label   (str email-count " emails about " system-call)}
                                 {:fx/type email-list}
                                 {:fx/type email-offset-button}]}}))

(defmethod views/panel :panel/kernel [_]
  {:fx/type       :v-box
   :style-class   ["panel"]
   :children      [{:fx/type :border-pane
                    :top     {:fx/type command-line}
                    :left    {:fx/type sidebar}
                    :center  {:fx/type system-call-emails}}]})
