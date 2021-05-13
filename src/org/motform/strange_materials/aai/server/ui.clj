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
     ::history {::alice []
                ::bob   []}}
    cache/lru-cache-factory)))

(defn completion [message]
  (let [{:keys [name prompt]} (edn/read-string message)]
    (-> (open-ai/completion-with :davinci-instruct-beta
          {:prompt prompt :max_tokens 64})
        util/realize
        open-ai/response-text
        str/triml)))

(defmethod event-handler ::server-request [{:keys [fx/context channel message]}]
  (tap> "SERVER getting request")
  {:context (fx/swap-context context assoc ::channel channel ::message message)})

(defmethod event-handler ::connect-server [{:keys [port dispatch!]}]
  (tap> "Connect to server")
  (server/start :port     port
                :dispatch (fn [message channel]
                            (dispatch! {:event/type ::server-request
                                        :channel    channel
                                        :message    message}))))

;;;; VIEWS

;;; Chat

(defn interception-view [{:keys [fx/context]}]
  {:fx/type  :v-box
   :children [{:fx/type :label :text "intercept"}]})

(defn chat-view [{:keys [accessor fx/context]}]
  {:fx/type  :v-box
   :children [{:fx/type :label :text "chat"}]})

(defn chat-panel [{:keys [fx/context]}]
  {:fx/type     :h-box
   :style-class "server-chat-panel"
   :children    [{:fx/type  chat-view
                  :accessor ::alice}
                 {:fx/type  interception-view}
                 {:fx/type  chat-view
                  :accessor ::bob}]})

;;; Server

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

;; todo add more status (connected clients?)
(defn server-status [{:keys [fx/context]}]
  (let [server? (fx/sub-ctx context sub-server?)]
    {:fx/type     :h-box
     :style-class "server-status"
     :children    [{:fx/type          :label 
                    :style-class      "server-status-connect"
                    :text             (if server? (str/upper-case "Connect to server") (str/upper-case "Server connected!"))
                    :on-mouse-clicked {:event/type ::start-server-connection}}]}))

;;;; RENDERER

(defn root [_]
  {:fx/type :stage
   :width   600
   :height  1200
   :showing true
   :title   "Man in the middle!"
   :scene   {:fx/type :scene
             :stylesheets [(::css/url styles/styles)]
             :root        {:fx/type     :border-pane
                           :style-class "pane"
                           :top         {:fx/type server-status}
                           :center      {:fx/type chat-panel}
                           }}})

(defn- random-port []
  (+ 8000 (rand-int 1000)))

(defn renderer [{:keys [port] :or {port (random-port)}}]
  (fx/create-renderer
   :middleware (comp fx/wrap-context-desc
                     (fx/wrap-map-desc #'root))
   :opts {:fx.opt/map-event-handler (-> event-handler
                                        (fx/wrap-co-effects
                                         {:fx/context (fx/make-deref-co-effect *state)})
                                        (fx/wrap-effects
                                         {:context       (fx/make-reset-effect *state)
                                          :dispatch       fx/dispatch-effect
                                          :server-connect (server-connect-effect port)}))
          :fx.opt/type->lifecycle #(or (fx/keyword->lifecycle %)
                                       (fx/fn->lifecycle-with-context %))}))

(defn -main [& args]
  (fx/mount-renderer *state (renderer args)))

(comment
  (swap! org.motform.strange-materials.aai.server.ui/*state identity)
  (-> @*state :cljfx.context/m ::channel)
  (-main)
  )
