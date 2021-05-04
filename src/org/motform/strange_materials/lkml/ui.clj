(ns org.motform.strange-materials.lkml.ui
  (:require [cljfx.api          :as fx]      
            [cljfx.css          :as css]
            [clojure.string     :as str]     
            [clojure.java.shell :as shell]
            [org.motform.strange-materials.lkml.db     :as db]
            [org.motform.strange-materials.lkml.styles :as styles]
            [org.motform.strange-materials.util        :as util])
  (:import [javafx.scene.input KeyCode KeyEvent]))

(defmulti event-handler :event/type)

(defmethod event-handler :default [event]
  (println "ERROR: Unknown event" (:event/type event))
  (println event))

;;; COMMAND LINE

(defn parse-strace [strace]
  (->> strace
       str/split-lines
       drop-last
       (map (fn [stack-frame] #:strace
              {:frame stack-frame
               :name (re-find #"^\w*" stack-frame)}))))

(defn linux-command [context]
  (fx/sub-val context :linux/command))

(defn linux-command-selected [context]
  (fx/sub-val context :linux.command/selected))

(defn linux-response [context]
  (let [id (fx/sub-ctx context linux-command-selected)]
    (get (fx/sub-val context :linux/responses) id {:error "incorrect command index"})))

(defn linux-responses [context]
  (fx/sub-val context :linux/responses))

(defn ssh-effect [{:keys [command]} dispatch!]
  (let [{:keys [out err]} (shell/sh "ssh" "vagrant@lkml" "strace" command)]
    (dispatch! {:event/type ::ssh-response
                :response   #:command{:output out
                                      :name   command
                                      :strace (parse-strace err)}})))

(defmethod event-handler ::ssh-response [{:keys [fx/context response]}]
  (let [id       (-> context :cljfx.context/m :linux/responses count)
        resp (assoc response :command/id id)]
    {:context (fx/swap-context context #(-> %
                                            (assoc-in [:linux/responses id] resp)
                                            (assoc :linux.command/selected  id)
                                            (assoc :linux/command "")
                                            (assoc :email/offest  50)))}))

(defmethod event-handler ::change-command [{:fx/keys [context event]}]
  {:context (fx/swap-context context assoc :linux/command event)})

(defmethod event-handler ::select-command-history [{:keys [fx/context id]}]
  {:context (fx/swap-context context assoc :linux.command/selected id)})

;; TODO Make this non-blocking! 
(defmethod event-handler ::submit-command [{:fx/keys [context event]}]
  (when (= KeyCode/ENTER (.getCode ^KeyEvent event))
    {:ssh {:command (fx/sub-ctx context linux-command)}}))

(defn command-history [{:fx/keys [context]}]
  (let [history          (vals (fx/sub-ctx context linux-responses))
        selected-command (fx/sub-ctx context linux-command-selected)]
    {:fx/type  :h-box
     :style-class "command-history"
     :children (for [{:command/keys [name id]} history]
                 {:fx/type          :label
                  :style-class      ["command-history-tab" (when (= id selected-command) "command-history-tab-selected")]
                  :on-mouse-clicked {:event/type  ::select-command-history
                                     :id          id}
                  :text             (-> name (str/split #" ") first)})}))

(defn command-line [{:fx/keys [context]}]
  (let [command   (fx/sub-ctx context linux-command)]
    {:fx/type     :v-box
     :style-class ["command-line"]
     :children [{:fx/type         :text-field
                 :on-text-changed {:event/type ::change-command}
                 :on-key-pressed  {:event/type ::submit-command}
                 :text            command}
                {:fx/type command-history}]}))

;;; SIDEBAR

(defmethod event-handler ::select-system-call [{:keys [fx/context system-call]}]
  {:context (fx/swap-context context assoc :selected-system-call system-call)})

(defn sidebar-container-label [{:keys [label]}]
  {:fx/type     :v-box
   :style-class "sidebar-container-label"
   :children    [{:fx/type     :label
                  :style-class "sidebar-container-label-text"
                  :text        (str/upper-case label)}]})

(defn selected-system-call [context]
  (fx/sub-val context :selected-system-call))

(defn system-call-item [{:keys [selected?] {:strace/keys [name]} :system-call}]
  {:fx/type          :h-box
   :style-class      (if selected? "system-call-item-selected" "system-call-item")
   :on-mouse-clicked {:event/type  ::select-system-call
                      :system-call name}
   :children [{:fx/type :label
               :text    (or name "")}]})

(defn system-call-view [{:fx/keys [context]}]
  (let [{:command/keys [strace]} (fx/sub-ctx context linux-response)
        selected-system-call     (fx/sub-ctx context selected-system-call)
        system-calls             (util/distinct-by-key strace :strace/name)]
    {:fx/type     :v-box
     :style-class "sidebar-container"
     :children    [{:fx/type sidebar-container-label
                    :label   "system calls"}
                   {:fx/type      :v-box
                    :style-class  "sidebar-list"
                    :spacing      3
                    :children     (for [{:strace/keys [name] :as system-call} system-calls]
                                    {:fx/type     system-call-item
                                     :system-call system-call
                                     :selected?   (= name selected-system-call)})}]}))

(defn std-out-item [{:keys [line]}]
  {:fx/type     :h-box
   :style-class "std-out-item"
   :children [{:fx/type     :label
               :style-class "std-out-item-text"
               :text        line}]})

(defn std-out-view [{:fx/keys [context]}]
  (let [{:command/keys [output]} (fx/sub-ctx context linux-response)]
    {:fx/type     :v-box
     :style-class "sidebar-container"
     :children    [{:fx/type sidebar-container-label
                    :label   "output"}
                   {:fx/type      :v-box
                    :style-class  ["sidebar-list" "std-out"]
                    :spacing      3
                    :children     (for [line (if output (str/split-lines output) [])]
                                    {:fx/type std-out-item
                                     :line    line})}]}))

(defn stack-trace-item [{:keys [selected?] {:strace/keys [name frame]} :system-call}]
  {:fx/type          :h-box
   :style-class      (if selected? "system-call-item-selected" "system-call-item")
   :on-mouse-clicked {:event/type  ::select-system-call
                      :system-call name}
   :children [{:fx/type :label
               :text    (or frame "")}]})

(defn stack-trace-view [{:fx/keys [context]}]
  (let [{:command/keys [strace]} (fx/sub-ctx context linux-response)
        selected-system-call     (fx/sub-ctx context selected-system-call)]
    {:fx/type     :v-box
     :style-class "sidebar-container"
     :children    [{:fx/type sidebar-container-label
                    :label   "stack trace"}
                   {:fx/type      :v-box
                    :style-class  "sidebar-list"
                    :spacing      3
                    :children     (for [{:strace/keys [name] :as system-call} strace]
                                    {:fx/type     stack-trace-item
                                     :system-call system-call
                                     :selected?   (= name selected-system-call)})}]}))

(defn sidebar [_]
  {:fx/type      :scroll-pane
   ;; :max-width     (/ (util/window-width) 3)
   :style-class  "sidebar"
   :fit-to-width true
   :content      {:fx/type :v-box
                  :children [{:fx/type std-out-view}
                             {:fx/type system-call-view}
                             {:fx/type stack-trace-view}]}})

;;; EMAILS

(defn email-empty [_]
  {:fx/type :label
   :text    ""})

(defn email-expanded [{{:email/keys [author address body]} :email}]
  (let [height (-> body str/split-lines count (* 17) (+ 25))]
    {:fx/type     :v-box
     :spacing     10
     :min-height  height
     :children    [{:fx/type     :label
                    :style-class "email-view-meta"
                    :text        (str (when author author) " " (when address (str "<" address ">")))}
                   {:fx/type     :label
                    :style-class "email-view-body"
                    :text        body}]}))

(defmethod event-handler ::select-email [{:keys [fx/context email]}]
  {:context (fx/swap-context context assoc :email/selected email)})

(defn email-view [{:keys [selected?] {:email/keys [subject rowid] :as email} :email}]
  {:fx/type          :v-box
   :style-class      ["email-view" (when selected? "email-view-selected")]
   :on-mouse-clicked {:event/type ::select-email
                      :email      rowid}
   :children         [{:fx/type     :label
                       :style-class (if selected? "email-view-topic" "")
                       :text        subject}
                      (if selected?
                        {:fx/type email-expanded
                         :email   email}
                        {:fx/type :v-box})]})

(defn selected-email [context]
  (fx/sub-val context :email/selected))

(defn email-offset [context]
  (fx/sub-val context :email/offset))

(defn email-list [{:fx/keys [context]}]
  (let [selected-email (fx/sub-ctx context selected-email)
        system-call    (fx/sub-ctx context selected-system-call)
        offset         (fx/sub-ctx context email-offset)
        emails         (if system-call
                         (remove #(str/blank? (:email/body %)) (db/emails-by-subject system-call 100 offset))
                         [])]
    {:fx/type  :v-box
     :children (if (empty? emails)
                 [{:fx/type email-empty}]
                 (for [email emails]
                   {:fx/type    email-view
                    :email      email
                    :selected?  (= (:email/rowid email) selected-email)}))}))

(defmethod event-handler ::increment-email-offset [{:fx/keys [context]}]
  {:context (fx/swap-context context update :email/offset + 20)})

(defn email-offset-button [_]
  {:fx/type          :label
   :style            {:-fx-alignment "CENTER"
                      :-fx-padding   15
                      :-fx-font-size 12}
   :on-mouse-clicked {:event/type  ::increment-email-offset}
   :text             (str/upper-case "load more emails")})

(defn system-call-emails [{:fx/keys [context]}]
  (let [system-call (fx/sub-ctx context selected-system-call)
        email-count (db/count-emails-by-subject system-call)]
    {:fx/type :v-box
     :children [{:fx/type sidebar-container-label
                 :label   (str email-count " emails about " system-call)}
                {:fx/type       :scroll-pane
                 :style-class   "email"
                 ;; :min-width     (* 2 (/ (util/window-width) 3))
                 :fit-to-width  true
                 :fit-to-height true
                 :content       {:fx/type   :v-box
                                 :children  [{:fx/type email-list}
                                             {:fx/type email-offset-button}]}}]}))

(defn root [_]
  {:fx/type :stage
   :width   960
   :height  540
   :showing true
   :title   "Excursion - The Linux Kernel Mailing List"
   :scene   {:fx/type :scene
             :stylesheets [(::css/url styles/styles)]
             :root        {:fx/type :border-pane
                           :top     {:fx/type command-line}
                           :left    {:fx/type sidebar}
                           :center  {:fx/type system-call-emails}}}})
