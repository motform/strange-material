(ns org.motform.strange-materials.aai.client.ui
  (:require [cljfx.api          :as fx]      
            [cljfx.css          :as css]
            [clojure.core.cache :as cache]
            [clojure.edn        :as edn]
            [clojure.string     :as str]
            [org.motform.strange-materials.util            :as util]
            [org.motform.strange-materials.aai.styles      :as styles]
            [org.motform.strange-materials.aai.client.core :as client])
  (:import [javafx.scene.input KeyCode KeyEvent]))

;;;; CLJFX
(defmulti event-handler :event/type)

(defmethod event-handler :default [event]
  (println "ERROR: Unknown event" (:event/type event))
  (println (pr-str event)))

(def *state
  (atom
   (fx/create-context
    {::client #:client{:id    (java.util.UUID/randomUUID)
                       ::name ""}
     ::server #:server{:socket false}
     ::chat   #:chat{:smart-replies ["Hi" "Hello" "Hola"]
                     :messages      []}}
    cache/lru-cache-factory)))

;;;; SUBS

(defn sub-socket        [context] (fx/sub-val context get-in [::server :server/socket]))
(defn sub-name          [context] (fx/sub-val context get-in [::client :client/name]))
(defn sub-id            [context] (fx/sub-val context get-in [::client :client/id]))
(defn sub-messages      [context] (fx/sub-val context get-in [::chat   :chat/messages]))
(defn sub-smart-replies [context] (fx/sub-val context get-in [::chat   :chat/smart-replies]))

;;;; EFFECTS

(defn ws-effect [{:keys [message/body message/type fx/context socket]} dispatch!]
  (let [socket (or socket (fx/sub-ctx context sub-socket))
        name   (fx/sub-ctx context sub-name)
        id     (fx/sub-ctx context sub-id)]
    (client/send-message socket #:message{:headers {:message/type type
                                                    :message/id   (java.util.UUID/randomUUID)
                                                    :client/name  name
                                                    :client/id    id}
                                          :body body})
    (dispatch! {:event/type ::update-name-input :fx/event ""})))

(defn ws-connect-effect [port]
  (fn [_ dispatch!]
    (dispatch! {:event/type ::connect-socket
                :port       port
                :dispatch!  dispatch!})))

;;;; EVENTS

(defmethod event-handler ::socket-response [{:keys [fx/context message]}]
  (case (-> message :message/headers :message/type)
    :message/smart-replies
    {:context (fx/swap-context context assoc-in [::chat :chat/smart-replies] (-> message :message/body))}
    :message/reply
    {:context (fx/swap-context context update-in [::chat :chat/messages] conj message)}))

(defmethod event-handler ::connect-socket [{:keys [fx/context dispatch! port]}]
  (letfn [(on-receive [message]
            (dispatch! {:event/type ::socket-response
                        :message    (edn/read-string message)}))]
    (let [socket (client/connect-socket port on-receive)]
      {:context (fx/swap-context context assoc-in [::server :server/socket] socket)
       :ws      {:fx/context context :message/type :message/handshake :socket socket}})))

(defmethod event-handler ::update-name-input [{:keys [fx/context fx/event]}]
  {:context (fx/swap-context context assoc ::input event)})

(defmethod event-handler ::submit-smart-reply [{:keys [fx/context smart-reply]}]
  {:ws {:fx/context   context
        :message/type :message/reply
        :message/body smart-reply}
   :context (fx/swap-context context assoc-in [::chat :chat/smart-replies] [])})

(defmethod event-handler ::submit-name [{:keys [fx/context fx/event]}]
  (when (= KeyCode/ENTER (.getCode ^KeyEvent event))
    {:ws-connect {:fx/context context}}))

(defmethod event-handler ::update-name [{:keys [fx/context fx/event]}]
  {:context (fx/swap-context context assoc-in [::client :client/name] event)})

;;;; VIEWS

;;; Chat

(defn chat-message [{:keys [message]}]
  {:fx/type :v-box
   :style-class "server-chat-view-message-container"
   :children [{:fx/type     :label
               :style-class "server-chat-view-sender"
               :text        (-> message :message/headers :client/name)}
              {:fx/type     :label
               :style-class "server-chat-view-message"
               :text        (-> message :message/body)}]})

(defn chat-view [{:keys [fx/context]}]
  (let [socket?  (fx/sub-ctx context sub-socket)
        messages (fx/sub-ctx context sub-messages)]
    (if socket?
      {:fx/type :scroll-pane
       :content {:fx/type     :v-box
                 :style-class "chat-messages"
                 :children    (if-not (empty? messages)
                                (for [message messages]
                                  {:fx/type     chat-message
                                   :style-class "chat-messages-message"
                                   :message     message})
                                [{:fx/type util/empty-view}])}}
      {:fx/type util/empty-view})))

(defn smart-reply-view [{:keys [fx/context]}]
  (let [smart-replies (fx/sub-ctx context sub-smart-replies)
        socket?       (fx/sub-ctx context sub-socket)]
    (if socket?
      {:fx/type     :h-box
       :style-class "chat-smart-reply-container"
       :children    (if-not (empty? smart-replies)
                      (for [smart-reply smart-replies]
                        {:fx/type          :label
                         :text             smart-reply
                         :style-class      "chat-smart-reply"
                         :on-mouse-clicked {:event/type ::submit-smart-reply
                                            :smart-reply smart-reply}})
                      [{:fx/type :label
                        :style-class      "chat-smart-loading"
                        :text "AI is loading new smart responses..."}])}
      {:fx/type util/empty-view})))

(defn name-input [{:keys [fx/context]}]
  (let [name    (fx/sub-ctx context sub-name)
        socket? (fx/sub-ctx context sub-socket)]
    (if-not socket?
      {:fx/type :v-box
       :style-class "chat-input-container"
       :children [{:fx/type     :label
                   :style-class "chat-input-label"
                   :text        (str/upper-case "Enter your name")}
                  {:fx/type         :text-field
                   :style-class     "chat-input-field"
                   :text            name
                   :on-key-pressed  {:event/type ::submit-name}
                   :on-text-changed {:event/type ::update-name}}]}
      {:fx/type :v-box
       :style-class "server-status"
       :children [{:fx/type     :label
                   :style-class "server-status-channel"
                   :text        "Chatting with Bob"}]})))

;;;; RENDERER

(defn root [_]
  {:fx/type :stage
   :width   600
   :height  1200
   :showing true
   :title   "I can't Believe its not LinkedIn"
   :scene   {:fx/type :scene
             :stylesheets [(::css/url styles/styles)]
             :root        {:fx/type     :border-pane
                           :style-class "pane"
                           :top         {:fx/type name-input}
                           :center      {:fx/type chat-view}
                           :bottom      {:fx/type smart-reply-view}}}})

(defn renderer [{:keys [port] :or {port 8080}}]
  (fx/create-renderer
   :middleware (comp fx/wrap-context-desc
                     (fx/wrap-map-desc #'root))
   :opts {:fx.opt/map-event-handler (-> event-handler
                                        (fx/wrap-co-effects
                                         {:fx/context (fx/make-deref-co-effect *state)})
                                        (fx/wrap-effects
                                         {:context    (fx/make-reset-effect *state)
                                          :dispatch   fx/dispatch-effect
                                          :ws         ws-effect
                                          :ws-connect (ws-connect-effect port)}))
          :fx.opt/type->lifecycle #(or (fx/keyword->lifecycle %)
                                       (fx/fn->lifecycle-with-context %))}))

(defn -main [& args]
  (fx/mount-renderer *state (renderer args)))

(comment
  (swap! org.motform.strange-materials.aai.client.ui/*state identity)

  (-> @*state :cljfx.context/m ::chat :chat/messages last)

  (-main :port 8887)
  )
