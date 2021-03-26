(ns motform.strange.material.editor
  (:require
   [byte-streams       :as byte]
   [cljfx.api          :as fx]
   [clj-tcp.client     :as tcp]
   [cljs.repl          :as repl]
   [cljs.repl.browser  :as browser]
   [clojure.core.async :as async]
   [clojure.edn        :as edn]
   [motform.strange.material.events :as events]
   [motform.strange.material.views  :as views])
  (:import [java.util UUID]
           [javafx.scene.input KeyCode KeyEvent]))

;;; TCP

(def c (tcp/client "127.0.0.1" 5555 {}))

(defn decode-result [result]
  (edn/read-string (byte/convert result String)))

(defn send-form! [form]
  (tcp/write! c (.getBytes form)) ; not sure that we really need to `.getBytes` 
  (decode-result (tcp/read! c)))

(defn tcp-effect [{:keys [form on-response]} dispatch!]
  (let [response (send-form! form)]
    (dispatch! (assoc on-response :response response))))

;;; SUBS

(defn repl-history [context]
  (fx/sub-val context :repl/history))

(defn repl-response [context]
  (fx/sub-val context :repl/response))

;;; EVENTS

(defmethod events/event-handler ::type-text [{:fx/keys [event context]}]
  {:context (fx/swap-context context assoc :repl/history event)})

(defmethod events/event-handler ::repl-response [{:keys [fx/context response]}]
  {:context (fx/swap-context context assoc :repl/response response)})

(defmethod events/event-handler ::submit-form [{:keys [fx/context]}]
  (let [form (fx/sub-ctx context repl-history)
        id   (UUID/randomUUID)]
    {:tcp {:form form
           :on-response {:event/type ::repl-response
                         :request-id id
                         :result :success}}}))

;;; VIEWS

;; text-area might be too simple for an editor
;; as it only supports :text from a string?
(defn editor [{:keys [fx/context]}]
  {:fx/type         :text-area
   :style-class     ["repl"]
   :text            (fx/sub-ctx context repl-history)
   :on-text-changed {:event/type ::type-text}})

(defn submit-form [{:fx/keys [context]}]
  {:fx/type     :button
   :text        "Submit"
   :style-class "repl-submit"
   :on-action   {:event/type ::submit-form}})

(defn response [{:fx/keys [context]}]
  (let [{:keys [val]} (fx/sub-ctx context repl-response)]
    {:fx/type :label
     :text    (str (or val "ERROR: response unavailable."))}))

(defmethod views/panel :panel/repl [_]
  {:fx/type     :v-box
   :style-class ["panel"]
   :children    [{:fx/type :label
                  :text    "REPL!"}
                 {:fx/type editor}
                 {:fx/type submit-form}
                 {:fx/type response}]})


(comment

  ;; clj-tcp
  (def c (tcp/client "127.0.0.1" 5555 {}))

  (tcp/read-print-in c)
  (tcp/write! c (.getBytes "(swap! state inc)"))

  (def f (future (tcp/read! c)))
  (realized? f)
  (decode-result f)
  
  (def reading? (atom false))
  (async/thread
    (while reading?
      (-> c
          tcp/read!
          (byte/convert String)
          edn/read-string)))


  )
