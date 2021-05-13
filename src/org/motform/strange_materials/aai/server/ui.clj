(ns org.motform.strange-materials.aai.server.ui
  (:require [cljfx.api :as fx]      
            [cljfx.css :as css]
            [clojure.edn        :as edn]
            [clojure.core.cache :as cache]
            [clojure.string     :as str]
            [org.motform.strange-materials.util               :as util]
            [org.motform.strange-materials.aai.styles         :as styles] ; NOTE shared w/ UI
            [org.motform.strange-materials.aai.server.core    :as server]
            [org.motform.strange-materials.aai.server.open-ai :as open-ai]))

;;;; CLJFX

(defmulti event-handler :event/type)

(defmethod event-handler :default [event]
  (println "ERROR: Unknown event" (:event/type event))
  (println event))

(def *state
  (atom
   (fx/create-context
    {::server? false
     ::clients {}
     ::prompt  nil} ; if not nil #{:client/id :prompt/clean :prompt/dirty}
    cache/lru-cache-factory)))

;;;; VIEWS

(defn completion [message]
  (let [{:keys [name prompt]} (edn/read-string message)]
    (-> (open-ai/completion-with :davinci-instruct-beta
          {:prompt prompt :max_tokens 64})
        util/realize
        open-ai/response-text
        str/triml)))

(defmethod event-handler ::server-request [{:keys [fx/context channel message]}]
  (let [{:message/keys [headers body]} message]
    (case (:message/type headers)
      :message/prompt
      {:context (fx/swap-context context assoc ::prompt {:client/id    (:client/id headers)
                                                         :prompt/dirty body
                                                         :prompt/clean body})}
      :message/handshake
      {:context (fx/swap-context context assoc-in [::clients (:client/id headers)] #:client{:channel  channel
                                                                                            :id       (:client/id   headers)
                                                                                            :name     (:client/name headers)
                                                                                            :messages []})})))

;;; Interception

(defn sub-prompt  [context] (fx/sub-val context ::prompt))
(defn sub-clients [context] (fx/sub-val context ::clients))

(defn server-send-effect [{:keys [channel clean dirty sender]} _]
  (server/send-response channel {:clean clean :dirty dirty :sender sender}))

(defn- other-client [clients id]
  (->> (dissoc clients id)
       vals
       (map :client/id)
       (remove nil?)
       first))

(defmethod event-handler ::send-completions [{:keys [fx/context]}]
  (let [{:keys [prompt/dirty prompt/clean client/id]} (fx/sub-ctx context sub-prompt)
        clients   (fx/sub-ctx context sub-clients)
        recipient (other-client clients id)]
    (def i id)
    (def c clients)
    {:server-send {:dirty   dirty
                   :clean   clean
                   :channel (get-in clients [id :client/channel])
                   :sender  (get-in clients [id :client/name])}
     :context     (-> context
                      (fx/swap-context assoc ::prompt nil)
                      (fx/swap-context update-in [::clients id :client/messages] conj clean)
                      (fx/swap-context update-in [::clients recipient :client/messages] conj dirty))}))

(defmethod event-handler ::update-dirty-prompt [{:keys [fx/context fx/event]}]
  {:context (fx/swap-context context assoc-in [::prompt :prompt/dirty] event)})

(defn prompt-editor [{:keys [fx/context sender]}]
  (let [{:prompt/keys [clean dirty]} (fx/sub-ctx context sub-prompt)]
    {:fx/type     :v-box
     :style-class "server-interception-editor"
     :children    [{:fx/type :v-box
                    :style-class "server-interception-editor-section"
                    :spacing 10
                    :children [{:fx/type     :label
                                :style-class "server-interception-editor-label"
                                :text        (str "Prompt from " sender)}
                               {:fx/type     :label
                                :style-class "server-interception-editor-prompt"
                                :text        clean}
                               {:fx/type     :label
                                :style-class "server-interception-editor-label"
                                :text        "Clean completion "}
                               {:fx/type     :label
                                :style-class "server-interception-editor-prompt"
                                :text        "huzzaz"}]}
                   {:fx/type :v-box
                    :spacing 10
                    :style-class "server-interception-editor-section"
                    :children [{:fx/type     :label
                                :style-class "server-interception-editor-label" 
                                :text        "Prompt editor"}
                               {:fx/type         :text-field
                                :style-class     "server-interception-editor-input"
                                :text            dirty
                                :on-text-changed {:event/type ::update-dirty-prompt}}
                               {:fx/type     :label
                                :style-class "server-interception-editor-label" 
                                :text        "Dirty completion"}
                               {:fx/type :label
                                :style-class     "server-interception-editor-prompt"
                                :text    "ooh no"}]}
                   {:fx/type          :label
                    :text             "Send message"
                    :style-class      "server-interception-editor-submit"
                    :on-mouse-clicked {:event/type ::send-completions}}]}))

(defn sub-sender [context]
  (get-in (fx/sub-val context ::clients) [(fx/sub-val context get-in [::prompt :client/id]) :client/name]))

(defn interception-view [{:keys [fx/context]}]
  (let [sender (fx/sub-ctx context sub-sender)]
    {:fx/type     :v-box
     :min-width   600
     :style-class "server-interception-container"
     :children    [{:fx/type     :label
                    :padding 10
                    :style-class "server-chat-view-name"
                    :text        (str/upper-case "interception")}
                   (if sender
                     {:fx/type prompt-editor
                      :sender  sender}
                     {:fx/type :label
                      :padding 10
                      :text    "Waiting for prompt."})]}))

;;; Chat

(defn chat-view [{{:client/keys [name messages]} :client}]
  {:fx/type     :v-box
   :min-width   200
   :style-class "server-chat-view"
   :children    (concat [{:fx/type     :label
                          :style-class "server-chat-view-name"
                          :text        (str/upper-case (or name "awaiting client"))}]
                        (if-not (empty? messages)
                          (for [message messages]
                            {:fx/type     :label
                             :style-class "server-chat-view-message"
                             :text        message})
                          [{:fx/type util/empty-view}]))})

(defn chat-panel [{:keys [fx/context]}]
  (let [clients (fx/sub-ctx context sub-clients)
        [c1 c2] (-> clients vals)]
    {:fx/type     :grid-pane
     :style-class "server-chat-panel"
     ;; :column-constraints (repeat 3 {:fx/type :column-constraints :percent-width 100/3})
     :children    [{:fx/type chat-view
                    :client  c1
                    :grid-pane/column 1 :grid-pane/vgrow :always}
                   {:fx/type  interception-view
                    :grid-pane/column 2 :grid-pane/vgrow :always}
                   {:fx/type chat-view
                    :client  c2
                    :grid-pane/column 3 :grid-pane/vgrow :always}]}))

;;; Server

(defmethod event-handler ::connect-server [{:keys [port dispatch!]}]
  (tap> "Connect to server")
  (server/start :port     port
                :dispatch (fn [message channel]
                            (dispatch! {:event/type ::server-request
                                        :channel    channel
                                        :message    message}))))

(defn server-connect-effect [port]
  (fn [_ dispatch!]
    (tap> "server-connect-effect")
    (dispatch! {:event/type ::connect-server
                :port       port
                :dispatch!  dispatch!})))

(defmethod event-handler ::start-server-connection [{:keys [fx/context]}]
  (tap> "SERVER getting request")
  {:context        (fx/swap-context context assoc ::server? true)
   :server-connect {}})

(defn sub-server? [context]
  (fx/sub-val context ::server?))

(defn server-connection-status [{:keys [fx/context]}]
  (let [server? (fx/sub-ctx context sub-server?)]
    {:fx/type          :label 
     :style-class      "server-status-connect"
     :text             (if server? (str/upper-case "Server connected!") (str/upper-case "Connect to server"))
     :on-mouse-clicked {:event/type ::start-server-connection}}))

(defn server-status [_]
  {:fx/type     :h-box
   :style-class "server-status"
   :children    (concat [{:fx/type server-connection-status}]
                        (for [channel @server/channels] ; BUG does not work bc it comes from an outside data source
                          {:fx/type     :label
                           :style-class "server-status-channel"
                           :text        (str channel)}))})

;;;; RENDERER

(defn root [_]
  {:fx/type :stage
   :width   1000
   :height  1200
   :showing true
   :title   "Man in the middle!"
   :scene   {:fx/type :scene
             :stylesheets [(::css/url styles/styles)]
             :root        {:fx/type     :border-pane
                           :style-class "pane"
                           :top         {:fx/type server-status}
                           :center      {:fx/type chat-panel}}}})

(defn renderer [{:keys [port] :or {port (util/random-port)}}]
  (fx/create-renderer
   :middleware (comp fx/wrap-context-desc
                     (fx/wrap-map-desc #'root))
   :opts {:fx.opt/map-event-handler (-> event-handler
                                        (fx/wrap-co-effects
                                         {:fx/context (fx/make-deref-co-effect *state)})
                                        (fx/wrap-effects
                                         {:context        (fx/make-reset-effect *state)
                                          :dispatch       fx/dispatch-effect
                                          :server-send    server-send-effect
                                          :server-connect (server-connect-effect port)}))
          :fx.opt/type->lifecycle #(or (fx/keyword->lifecycle %)
                                       (fx/fn->lifecycle-with-context %))}))

(defn -main [& args]
  (fx/mount-renderer *state (renderer args)))

(comment
  (swap! org.motform.strange-materials.aai.server.ui/*state identity)
  (-> @*state :cljfx.context/m ::clients)

  (-main :port 8891)
  )
