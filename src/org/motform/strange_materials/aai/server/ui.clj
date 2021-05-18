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

(def empty-smart-reply {:client/id nil :smart-reply/drafts ["" "" ""]})

(def *state
  (atom
   (fx/create-context
    {::server?  false
     ::messages []         #_[{:client/name "Alice" :completion/response "Hello Bob!"}]
     ::clients  {}         #_{1 #:client{:channel nil :id 1 :name "Alice" :messages ["hi"]}}
     ::smart-reply-request empty-smart-reply}
    cache/lru-cache-factory)))

;;;; SUBS

(defn sub-clients  [context] (fx/sub-val context ::clients))
(defn sub-messages [context] (fx/sub-val context ::messages))
(defn sub-server?  [context] (fx/sub-val context ::server?))
(defn sub-smart-reply-request [context] (fx/sub-val context ::smart-reply-request))

;;;; EFFECTS

(defn completion-effect [{:keys [client/prompt]} dispatch!]
  (dispatch! {:event/type ::completion-response
              :response   (-> (open-ai/completion-with :davinci-instruct-beta ; are we using the wrong model?
                                {:prompt prompt :max_tokens 64})
                              util/realize
                              open-ai/response-text
                              str/triml)}))

(defn server-notify-all [{:keys [message]} _]
  (server/notify-clients message))

(defn server-send-smart-replies-effect [{:keys [channel smart-replies]} _]
  (server/send-message channel #:message{:headers {:message/id   (java.util.UUID/randomUUID)
                                                   :message/type :message/smart-replies}
                                         :body smart-replies}))

(defn server-connect-effect [port]
  (fn [_ dispatch!]
    (tap> "server-connect-effect")
    (dispatch! {:event/type ::connect-server
                :port       port
                :dispatch!  dispatch!})))

;;;; EVENTS

(defmethod event-handler ::completion-response [{:keys [fx/context response]}]
  {:context #_(fx/swap-context context assoc ::completion response)
   (fx/swap-context context assoc-in [::prompt :completion/response] response)})

(defmethod event-handler ::completion-request [{:keys [server/prompt]}]
  {:completion/request {:prompt prompt}})

(defmethod event-handler ::server-request [{:keys [fx/context channel message]}]
  (case (-> message :message/headers :message/type)
    :message/reply
    {:server/notify-all {:message message}
     :context (-> context
                  (fx/swap-context assoc-in [::smart-reply-request :client/id] (-> message :message/headers :client/id))
                  (fx/swap-context update ::messages conj message))}

    :message/handshake
    {:context (fx/swap-context context assoc-in [::clients (-> message :message/headers :client/id)]
                               #:client{:channel  channel
                                        :id       (-> message :message/headers :client/id)
                                        :name     (-> message :message/headers :client/name)
                                        :messages []})}))

(defmethod event-handler ::send-smart-replies [{:keys [fx/context client/id]}]
  (let [{:keys [smart-reply/drafts]} (fx/sub-ctx context sub-smart-reply-request)
        channel (get-in (fx/sub-ctx context sub-clients) [id :client/channel])]
    {:server/send-smart-replies {:smart-replies drafts
                                 :channel       channel}
     :context (fx/swap-context context assoc ::smart-reply-request empty-smart-reply)}))

(defmethod event-handler ::edit-smart-reply [{:keys [fx/context fx/event index]}]
  {:context (fx/swap-context context assoc-in [::smart-reply-request :smart-reply/drafts index] event)})

(defmethod event-handler ::connect-server [{:keys [port dispatch!]}]
  (tap> "Connect to server")
  (server/start :port     port
                :dispatch (fn [message channel]
                            (dispatch! {:event/type ::server-request
                                        :channel    channel
                                        :message    message}))))

(defmethod event-handler ::start-server-connection [{:keys [fx/context]}]
  (tap> "SERVER getting request")
  {:context        (fx/swap-context context assoc ::server? true)
   :server/connect {}})


;;;; VIEWS


(defn smart-reply-field [{:keys [drafts index]}]
  {:fx/type     :v-box
   :style-class "server-interception-editor-prompt-editable-container"
   :children    [{:fx/type         :text-field
                  :style-class     "server-interception-editor-prompt-editable"
                  :text            (drafts index)
                  :on-text-changed {:event/type ::edit-smart-reply
                                    :index      index}}]})

(defn prompt-editor [{:keys [fx/context]}]
  (let [{:keys [client/id smart-reply/drafts]} (fx/sub-ctx context sub-smart-reply-request)
        client-name (get-in (fx/sub-ctx context sub-clients) [id :client/name])]
    {:fx/type     :v-box
     :min-width   400
     :style-class "server-interception-editor"
     :children    [{:fx/type :v-box
                    :style-class "server-interception-section"
                    :children [{:fx/type     :label
                                :style-class "server-interception-editor-label"
                                :text        (str/upper-case (str "Smart replies for " client-name))}
                               {:fx/type smart-reply-field :index 0 :drafts drafts}
                               {:fx/type smart-reply-field :index 1 :drafts drafts}
                               {:fx/type smart-reply-field :index 2 :drafts drafts}]}
                   {:fx/type          :label
                    :text             (str/upper-case "Send smart replies")
                    :style-class      "server-interception-editor-submit"
                    :on-mouse-clicked {:event/type ::send-smart-replies
                                       :client/id  id}}]}))

(defn interception-view [{:keys [fx/context]}]
  (let [{:keys [client/id]} (fx/sub-ctx context sub-smart-reply-request)]
    {:fx/type     :v-box
     :min-width   400
     :style-class "server-interception-container"
     :children    [(if id
                     {:fx/type   prompt-editor}
                     {:fx/type :label
                      :padding 10
                      :text    "Waiting for client to request smart reply..."})]}))

;;; Chat

(defn chat-view [{:keys [fx/context]}]
  (let [messages (fx/sub-ctx context sub-messages)]
    {:fx/type     :v-box
     :min-width   300
     :style-class "server-chat-view"
     :children    (if-not (empty? messages)
                    (for [{:message/keys [headers body]} messages]
                      {:fx/type     :v-box
                       :style-class "server-chat-view-message-container"
                       :children    [{:fx/type     :label
                                      :style-class "server-chat-view-sender"
                                      :text        (or (:client/name headers) "Placeholder name")}
                                     {:fx/type     :label
                                      :style-class "server-chat-view-message"
                                      :text        (util/break-lines body 30)}]})
                    [{:fx/type util/empty-view}])}))

;;; Server

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
                                         {:context                   (fx/make-reset-effect *state)
                                          :dispatch                  fx/dispatch-effect
                                          :completion/request        completion-effect
                                          :server/send-smart-replies server-send-smart-replies-effect
                                          :server/notify-all         server-notify-all
                                          :server/connect            (server-connect-effect port)}))
          :fx.opt/type->lifecycle #(or (fx/keyword->lifecycle %)
                                       (fx/fn->lifecycle-with-context %))}))

(defn -main [& args]
  (fx/mount-renderer *state (renderer args)))

(comment
  (swap! org.motform.strange-materials.aai.server.ui/*state identity)
  (-> @*state :cljfx.context/m ::smart-reply-request )

  (-main :port 8883)
  )
