(ns org.motform.strange-materials.aai.client.ui
  (:require [cljfx.api          :as fx]      
            [cljfx.css          :as css]
            [clojure.core.cache :as cache]
            [clojure.string     :as str]
            [org.motform.strange-materials.aai.client.core   :as client]
            [org.motform.strange-materials.aai.client.styles :as styles])
  (:import [javafx.scene.input KeyCode KeyEvent]))

;;;; CLJFX

(defmulti event-handler :event/type)

(defmethod event-handler :default [event]
  (println "ERROR: Unknown event" (:event/type event))
  (println event))

(def *state
  (atom
   (fx/create-context
    {::socket  false
     ::history [#:message{:author "John Done" :text "Hello!"}
                #:message{:author "Me"        :text "Oh hi Mark!"}
                #:message{:author "John Done" :text "My name is not Mark."}]
     ::input   ""
     ::quota   {:total 100
                :spent 20}}
    cache/lru-cache-factory)))

;;;; VIEWS

;;; Tokens

(defn sub-quota [context]
  (fx/sub-val context ::quota))

(defn quota-meter [{:keys [fx/context]}]
  (let [{:keys [total spent]} (fx/sub-ctx context sub-quota)]
    {:fx/type     :v-box
     :style-class "quota-container"
     :padding     10
     :children    [{:fx/type :label
                    :text    (str "You have spent " spent " out of " total " tokens.")}]}))

;;; Chat

(defn sub-socket [context]
  (fx/sub-val context ::socket))

(defmethod event-handler ::socket-response [{:keys [fx/context message] :as m}]
  (def m m)
  {:context (fx/swap-context context update ::history conj message)})

(defmethod event-handler ::connect-socket [{:keys [fx/context dispatch!]}]
  (tap> "Connect socket")
  (letfn [(on-receive [message]
            (dispatch! {:event/type ::socket-response
                        :message  #:message{:text message :author "John Doe"}}))]
    {:context (fx/swap-context context assoc ::socket (client/connect-socket on-receive))}))

(defn ws-effect [{:keys [prompt fx/context]} dispatch!]
  (tap> "ws-effect")
  (let [socket (fx/sub-ctx context sub-socket)]
    (if-not socket
      (do (dispatch! {:event/type ::connect-socket :dispatch! dispatch!})
          (dispatch! {:event/type ::submit-message :triggered? true}))
      (do (client/send-prompt socket prompt)
          (dispatch! {:event/type ::update-input :fx/event ""}))))) ; XXX not sure this works

(defn chat-message [{{:message/keys [author text]} :message}]
  {:fx/type     :v-box
   :style-class "chat-message-container"
   :children    [{:fx/type     :v-box
                  :style-class "chat-message"
                  :children    [{:fx/type     :label
                                 :style-class "chat-message-author"
                                 :text        author}
                                {:fx/type :label
                                 :text    text}]}]})

(defn sub-history [context]
  (fx/sub-val context ::history))

(defn chat-history [{:keys [fx/context]}]
  (let [history (fx/sub-ctx context sub-history)]
    {:fx/type  :v-box
     :children (for [message history]
                 {:fx/type chat-message
                  :message message})}))

(defn sub-input [context]
  (fx/sub-val context ::input))

(defmethod event-handler ::update-input [{:keys [fx/context fx/event]}]
  {:context (fx/swap-context context assoc ::input event)})

(defmethod event-handler ::submit-message [{:keys [fx/context fx/event triggered?]}]
  (when (or triggered? (try (= KeyCode/ENTER (.getCode ^KeyEvent event))
                            (catch Exception e (str "ex: " (.getMessage e)))))
    {:ws {:fx/context context
          :prompt (fx/sub-ctx context sub-input)}}))

(defn chat-input [{:keys [fx/context]}]
  (let [input (fx/sub-ctx context sub-input)]
    {:fx/type     :v-box
     :style-class "chat-input-container"
     :children    [{:fx/type     :text-field
                    :style-class "chat-input-field"
                    :text        input
                    :on-key-pressed  {:event/type ::submit-message}
                    :on-text-changed {:event/type ::update-input}}]}))

(defn chat-container [_]
  {:fx/type     :v-box
   :style-class "chat-container"
   :children    [{:fx/type chat-history}
                 {:fx/type chat-input}]})

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
                           :top         {:fx/type quota-meter}
                           :center      {:fx/type chat-container}}}})

(def renderer
  (fx/create-renderer
   :middleware (comp fx/wrap-context-desc
                     (fx/wrap-map-desc #'root))
   :opts {:fx.opt/map-event-handler (-> event-handler
                                        (fx/wrap-co-effects
                                         {:fx/context (fx/make-deref-co-effect *state)})
                                        (fx/wrap-effects
                                         {:context  (fx/make-reset-effect *state)
                                          :dispatch  fx/dispatch-effect
                                          :ws        ws-effect}))
          :fx.opt/type->lifecycle #(or (fx/keyword->lifecycle %)
                                       (fx/fn->lifecycle-with-context %))}))

(defn -main [& _]
  (fx/mount-renderer *state renderer))

(comment
  (swap! org.motform.strange-materials.aai.client.ui/*state identity)

  (-> @*state :cljfx.context/m ::history)

  (-main)
  )
