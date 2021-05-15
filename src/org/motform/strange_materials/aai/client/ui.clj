(ns org.motform.strange-materials.aai.client.ui
  (:require [cljfx.api          :as fx]      
            [cljfx.css          :as css]
            [clojure.core.cache :as cache]
            [clojure.edn        :as edn]
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
    {::socket  false
     ::id      (java.util.UUID/randomUUID)
     ::name    ""
     ::history []
     ::input   ""
     ::quota   {:total 100
                :spent 20}}
    cache/lru-cache-factory)))

;;;; VIEWS

;;; Tokens

(defn sub-socket [context] (fx/sub-val context ::socket))
(defn sub-quota  [context] (fx/sub-val context ::quota))
(defn sub-name   [context] (fx/sub-val context ::name))

(defn quota-meter [{:keys [fx/context]}]
  (let [name                  (fx/sub-ctx context sub-name)
        {:keys [total spent]} (fx/sub-ctx context sub-quota)
        socket?               (fx/sub-ctx context sub-socket)]
    (if socket?
      {:fx/type     :v-box
       :style-class "quota-container"
       :padding     10
       :spacing     10
       :children    [{:fx/type :label
                      :text     (str "Hello, " name)}
                     {:fx/type :label
                      :text    (str "You have spent " spent " out of " total " tokens.")}]}
      {:fx/type util/empty-view})))

;;; Chat

(defmethod event-handler ::socket-response [{:keys [fx/context message]}]
  {:context (fx/swap-context context update ::history conj message)})

(defmethod event-handler ::connect-socket [{:keys [fx/context dispatch! port]}]
  (tap> "Connect socket")
  (letfn [(on-receive [message]
            (dispatch! {:event/type ::socket-response
                        :message    (edn/read-string message)}))]
    (let [socket (client/connect-socket port on-receive)]
      {:context (fx/swap-context context assoc ::socket socket)
       :ws      {:fx/context context :message/type :message/handshake :socket socket}})))

(defn sub-id [context] (fx/sub-val context ::id))

(defn ws-effect [{:keys [message/body message/type fx/context socket]} dispatch!]
  (tap> "ws-effect")
  (let [socket (or socket (fx/sub-ctx context sub-socket))
        name   (fx/sub-ctx context sub-name)
        id     (fx/sub-ctx context sub-id)]
    (client/send-message socket #:message{:headers {:message/type type
                                                    :message/id   (java.util.UUID/randomUUID)
                                                    :client/name  name
                                                    :client/id    id}
                                          :body body})
    (dispatch! {:event/type ::update-input :fx/event ""})))

(defmethod event-handler ::submit-name [{:keys [fx/context fx/event]}]
  (when (= KeyCode/ENTER (.getCode ^KeyEvent event))
    {:ws-connect {:fx/context context}}))

(defn ws-connect-effect [port]
  (fn [_ dispatch!]
    (tap> "ws-connect-effect")
    (dispatch! {:event/type ::connect-socket
                :port       port
                :dispatch!  dispatch!})))

(defn chat-message [{:keys [message]}]
  {:fx/type     :v-box
   :style-class "chat-message-container"
   :children    [{:fx/type     :v-box
                  :style-class "chat-message"
                  :children    [{:fx/type     :label
                                 :style-class "chat-message-author"
                                 :text        (-> message :message/headers :sender/name)}
                                {:fx/type :label
                                 :text    (:message/body message)}]}]})

(defn sub-history [context] (fx/sub-val context ::history))

(defn chat-history [{:keys [fx/context]}]
  (let [history (fx/sub-ctx context sub-history)]
    {:fx/type  :v-box
     :children (for [message history]
                 {:fx/type chat-message
                  :message message})}))

(defn sub-input [context] (fx/sub-val context ::input))

(defmethod event-handler ::update-input [{:keys [fx/context fx/event]}]
  {:context (fx/swap-context context assoc ::input event)})

(defmethod event-handler ::submit-message [{:keys [fx/context fx/event]}]
  (when (= KeyCode/ENTER (.getCode ^KeyEvent event))
    {:ws {:fx/context context
          :message/type :message/prompt
          :message/body (fx/sub-ctx context sub-input)}}))

(defn chat-input [{:keys [fx/context]}]
  (let [input (fx/sub-ctx context sub-input)]
    {:fx/type     :v-box
     :style-class "chat-input-container"
     :children    [{:fx/type     :text-field
                    :style-class "chat-input-field"
                    :text        input
                    :on-key-pressed  {:event/type ::submit-message}
                    :on-text-changed {:event/type ::update-input}}]}))

(defn chat-container [{:keys [fx/context]}]
  (let [socket? (fx/sub-ctx context sub-socket)]
    (if socket?
      {:fx/type     :v-box
       :style-class "chat-container"
       :children    [{:fx/type chat-history}
                     {:fx/type chat-input}]}
      {:fx/type util/empty-view})))

(defmethod event-handler ::update-name [{:keys [fx/context fx/event]}]
  {:context (fx/swap-context context assoc ::name event)})

(defn name-input [{:keys [fx/context]}]
  (let [name    (fx/sub-ctx context sub-name)
        socket? (fx/sub-ctx context sub-socket)]
    (if-not socket?
      {:fx/type :v-box
       :style-class "chat-input-container"
       :children [{:fx/type     :label
                   :style-class "chat-message-author"
                   :text        "Enter your name"}
                  {:fx/type :text-field
                   :style-class "chat-input-field"
                   :text    name
                   :on-key-pressed  {:event/type ::submit-name}
                   :on-text-changed {:event/type ::update-name}}]}
      {:fx/type util/empty-view})))

;;;; RENDERER

(defn root [_]
  {:fx/type :stage
   :width   600
   :height  1200
   :showing true
   :title   "The Chat of Tomorrow… Today!"
   :scene   {:fx/type :scene
             :stylesheets [(::css/url styles/styles)]
             :root        {:fx/type     :border-pane
                           :style-class "pane"
                           :top         {:fx/type name-input}
                           :bottom      {:fx/type quota-meter}
                           :center      {:fx/type chat-container}}}})

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

  (-> @*state :cljfx.context/m ::history)

  (-main :port 8891)
  )
