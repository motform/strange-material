(ns org.motform.strange-materials.aai.server.ui
  (:require [cljfx.api :as fx]      
            [cljfx.css :as css]
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
     ::clients #_{} {1 #:client{:channel nil :id 1 :name "test" :messages ["hi"]}}
     ::prompt  #_nil {:client/id 1 :client/prompt "Test prompt." :server/prompt "Test prompt." :completion/response nil}
     } ; if not nil {:client/id :client/prompt :server/prompt :completion/response} - should spec this
    cache/lru-cache-factory)))

;;;; VIEWS

(defn completion-effect [{:keys [client/prompt]} dispatch!]
  (println "completion-effect" prompt)
  (def p prompt)
  (dispatch! {:event/type ::completion-response
              :response   (-> (open-ai/completion-with :davinci-instruct-beta
                                {:prompt prompt :max_tokens 64})
                              util/realize
                              open-ai/response-text
                              str/triml)}))

(defmethod event-handler ::completion-response [{:keys [fx/context response]}]
  (println "completion response")
  (def ctx context)
  (def r response)
  {:context #_(fx/swap-context context assoc ::completion response)
   (fx/swap-context context assoc-in [::prompt :completion/response] response)})

(defmethod event-handler ::completion-request [{:keys [client/prompt]}]
  {:completion/request {:prompt prompt}})

(defn server-send-effect [{:keys [channel completion/response client/name]} _]
  (server/send-response channel {:completion/response response :client/name name}))

(defmethod event-handler ::server-request [{:keys [fx/context channel message]}]
  (let [{:message/keys [headers body]} message]
    (case (:message/type headers)
      :message/prompt
      {:completion/request {:client/prompt body}
       :context (fx/swap-context context assoc ::prompt {:client/id     (:client/id headers)
                                                         :client/prompt body
                                                         :server/prompt body})}
      :message/handshake
      {:context (fx/swap-context context assoc-in [::clients (:client/id headers)] #:client{:channel  channel
                                                                                            :id       (:client/id   headers)
                                                                                            :name     (:client/name headers)
                                                                                            :messages []})})))

;;; Interception

(defn sub-prompt  [context] (fx/sub-val context ::prompt))
(defn sub-clients [context] (fx/sub-val context ::clients))

(defmethod event-handler ::send-completions [{:keys [fx/context]}]
  (let [{:client/keys [prompt id]} (fx/sub-ctx context sub-prompt)
        clients                    (fx/sub-ctx context sub-clients)]
    {:server/send {:client/prompt prompt ; TODO send the completion, not the prompt
                   :client/name  (get-in clients [id :client/name])
                   :channel      (get-in clients [id :client/channel])}
     :context     (-> context
                      (fx/swap-context assoc ::prompt nil)
                      (fx/swap-context update-in [::clients id :client/messages] conj prompt))}))

(defmethod event-handler ::edit-prompt [{:keys [fx/context fx/event]}]
  {:context (fx/swap-context context assoc-in [::prompt :server/prompt] event)})

;; TODO why does it not assoc the completion in to the state?
(defn prompt-editor [{:keys [fx/context sender]}]
  (let [prompt (fx/sub-ctx context sub-prompt)]
    {:fx/type     :v-box
     :style-class "server-interception-editor"
     :children    [{:fx/type :v-box
                    :min-width 400
                    :style-class "server-interception-section"
                    :children [{:fx/type :label :text (str/upper-case (str "prompt from " sender)) :style-class "server-interception-editor-label"}
                               {:fx/type     :label
                                :style-class "server-interception-editor-prompt-static"
                                :text        (:client/prompt prompt)}]}

                   {:fx/type     :v-box
                    :style-class "server-interception-section"
                    :children    [{:fx/type :label :text "EDIT PROMPT" :style-class "server-interception-editor-label"}
                                  {:fx/type     :v-box
                                   :style-class "server-interception-editor-prompt-editable-container"
                                   :children    [{:fx/type         :text-field
                                                  :style-class     "server-interception-editor-prompt-editable"
                                                  :text            (:server/prompt prompt)
                                                  :on-text-changed {:event/type ::edit-prompt}}]}]}

                   {:fx/type     :v-box
                    :style-class "server-interception-section"
                    :min-width 400
                    :children    [{:fx/type :label :text "COMPLETION" :style-class "server-interception-editor-label"}
                                  {:fx/type     :label
                                   :style-class "server-interception-editor-completion"
                                   :text        (or (:completion/response prompt) "Waiting for completion.")}]}

                   {:fx/type          :label
                    :text             "SEND MESSAGE"
                    :style-class      "server-interception-editor-submit"
                    :on-mouse-clicked {:event/type ::send-completions}}]}))

(defn sub-sender [context]
  (get-in (fx/sub-val context ::clients) [(fx/sub-val context get-in [::prompt :client/id]) :client/name]))

(defn interception-view [{:keys [fx/context]}]
  (let [sender (fx/sub-ctx context sub-sender)]
    {:fx/type     :v-box
     :min-width   400
     :style-class "server-interception-container"
     :children    [(if sender
                     {:fx/type prompt-editor
                      :sender  sender}
                     {:fx/type :label
                      :padding 10
                      :text    "Waiting for prompt."})]}))

;;; Chat

(defn chat-view [{:keys [fx/context]}]
  (let [clients (fx/sub-ctx context sub-clients)
        {:client/keys [messages]} (-> clients vals first)]
    {:fx/type     :v-box
     :min-width   200
     :style-class "server-chat-view"
     :children    (concat [{:fx/type     :label
                            :style-class "server-chat-view-name"
                            :text        (str/upper-case (or (when name "conversation") "awaiting client"))}]
                          (if-not (empty? messages)
                            (for [message messages]
                              {:fx/type     :label
                               :style-class "server-chat-view-message"
                               :text        message})
                            [{:fx/type util/empty-view}]))}))

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
   :server/connect {}})

(defn sub-server? [context]
  (fx/sub-val context ::server?))

(defn server-connection-status [{:keys [fx/context]}]
  (let [server? (fx/sub-ctx context sub-server?)]
    {:fx/type          :label 
     :style-class      "server-status-connect"
     :text             (if server? (str/upper-case "Server connected!") (str/upper-case "Launch server"))
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
   :title   "Artificial Artificial Intelligence"
   :scene   {:fx/type :scene
             :stylesheets [(::css/url styles/styles)]
             :root        {:fx/type     :border-pane
                           :style-class "pane"
                           :top         {:fx/type server-status}
                           :left        {:fx/type chat-view}
                           :center      {:fx/type interception-view}}}})

(defn renderer [{:keys [port] :or {port (util/random-port)}}]
  (fx/create-renderer
   :middleware (comp fx/wrap-context-desc
                     (fx/wrap-map-desc #'root))
   :opts {:fx.opt/map-event-handler (-> event-handler
                                        (fx/wrap-co-effects
                                         {:fx/context (fx/make-deref-co-effect *state)})
                                        (fx/wrap-effects
                                         {:context            (fx/make-reset-effect *state)
                                          :dispatch           fx/dispatch-effect
                                          :completion/request completion-effect
                                          :server/send        server-send-effect
                                          :server/connect     (server-connect-effect port)}))
          :fx.opt/type->lifecycle #(or (fx/keyword->lifecycle %)
                                       (fx/fn->lifecycle-with-context %))}))

(defn -main [& args]
  (fx/mount-renderer *state (renderer args)))

(comment
  (swap! org.motform.strange-materials.aai.server.ui/*state identity)
  (-> @*state :cljfx.context/m ::client)
  

  (-main :port 8895)
  )

