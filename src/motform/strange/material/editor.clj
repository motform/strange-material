(ns motform.strange.material.editor
  (:require
   [byte-streams                    :as byte]    
   [cljfx.api                       :as fx]      
   [clj-tcp.client                  :as tcp]     
   [cljs.repl                       :as repl]    
   [cljs.repl.browser               :as browser] 
   [clojure.core.async              :as async]   
   [clojure.edn                     :as edn]     
   [clojure.string                  :as str]     
   [motform.strange.material.events :as events]  
   [motform.strange.material.views  :as views])  
  (:import [java.util UUID]
           [java.util Date]
           [javafx.scene.input KeyCode KeyEvent]))

;;; TCP

(def *repl-client (atom nil))

(defn decode-result [result]
  (-> result
      (byte/convert String)
      (edn/read-string)
      (assoc :decoded (Date.))))

;; This should probably be moved to a direct queue
;; instead of interfacing with tcp/read!
(defn read-results! []
  (loop [results []]
    (if-let [r (tcp/read! @*repl-client 100)]
      (recur (conj results (decode-result r)))
      results)))

(defn send-forms! [forms]
  (doseq [form forms]
    (tcp/write! @*repl-client (.getBytes form))))

(defn setup-repl! [dispatch!]
  (reset! *repl-client (tcp/client "127.0.0.1" 5555 {:write-buff 50 :read-buff 50}))
  (async/go
    (while true
      (dispatch! {:event/type ::repl-response
                  :response (decode-result (async/<! (:read-ch @*repl-client)))}))))

(defn tcp-effect [{:keys [form on-response repl/active?]} dispatch!]
  (when-not @*repl-client (setup-repl! dispatch!))
  (send-forms! (str/split form #"\n+")))

;;; SUBS

(defn repl-history [context]
  (fx/sub-val context :repl/history))

(defn repl-active [context]
  (fx/sub-val context :repl/active?))

(defn repl-response [context]
  (fx/sub-val context :repl/responses))

;;; EVENTS

(defmethod events/event-handler ::type-text [{:fx/keys [event context]}]
  {:context (fx/swap-context context assoc :repl/history event)})

(defmethod events/event-handler ::repl-response [{:keys [fx/context response]}]
  {:context (fx/swap-context context update :repl/responses conj response)})

(defmethod events/event-handler ::submit-form [{:keys [fx/context]}]
  {:tcp {:form (fx/sub-ctx context repl-history)}})

;;; VIEWS

;; text-area might be too simple for an editor
;; as it only supports :text from a string?
(defn editor [{:keys [fx/context]}]
  {:fx/type        :text-area   
   :style-class     "repl"       
   :text            (fx/sub-ctx context repl-history) 
   :on-text-changed {:event/type ::type-text}}) 

(defn submit-form [{:fx/keys [context]}]
  {:fx/type     :button
   :text        "Submit"
   :style-class "repl-submit"
   :on-action   {:event/type ::submit-form}})

(defn tape [{:fx/keys [context]}]
  (let [responses (->> (fx/sub-ctx context repl-response)
                       (map-indexed (fn [i response] (str (inc i) ".     " (:val response))))
                       reverse
                       (take 10)
                       (str/join "\n\n"))]
    {:fx/type     :label
     :style-class [(when-not (empty? responses) "repl-tape")]
     :text        (str (or responses "ERROR: response unavailable."))}))

(defmethod views/panel :panel/repl [_]
  {:fx/type     :v-box
   :style-class ["panel"]
   :children    [{:fx/type :label
                  :text    "REPL!"}
                 {:fx/type editor}
                 {:fx/type submit-form}
                 {:fx/type tape}]})


(comment

  ;; clj-tcp
  ;; (def c' (tcp/client "127.0.0.1" 5555 {:read-timeout 10}))
  (tcp/write! c' (.getBytes "(+ 1 1)\n\n(+ 2 2)"))

  ;; (def r  (tcp/read! c' 100))
  ;; (when r (decode-result r))
  ;; (loop [acc []]
  ;;   (if-let [r (tcp/read! c' 100)]
  ;;     (recur (conj acc (decode-result r)))
  ;;     acc))

  ;; (async/<!!
  ;;  (async/thread
  ;;    (take-while #(> 10 %) (range 20))))

  (tcp/write! c (.getBytes "(+ 1 1)\n\n(+ 2 2)"))

  (async/go
    (while true
      (println (decode-result (async/<! (:read-ch c))))))

  ;; (defn read!
  ;;   "Reads from the read-ch and blocks if no data is available"
  ;;   ([{:keys [read-ch]} timeout-ms]
  ;;    (first
  ;;     (alts!!
  ;;      [read-ch
  ;;       (timeout timeout-ms)])))
  ;;   ([{:keys [read-ch]}]
  ;;    (<!! read-ch)))
  ;; (tcp/read!)

  (let [(async/chan 10)])


  )
