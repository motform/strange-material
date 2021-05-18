(ns org.motform.strange-materials.aai.server.core
  (:require [clojure.edn        :as edn]
            [org.httpkit.server :as server]
            [reitit.ring        :as ring]))

(set! *warn-on-reflection* true)

(defonce channels (atom #{}))

(defn- connect! [channel]
  (tap> "SERVER channel open")
  (swap! channels conj channel))

(defn- disconnect! [channel status]
  (tap> (str "SERVER channel closed: " status))
  (swap! channels #(remove #{channel} %)))

(defn notify-clients [message]
  (doseq [channel @channels]
    (server/send! channel (pr-str message))))

(defn notify-other-clients [excluded-channel message]
  (doseq [channel (remove #{excluded-channel} @channels)]
    (server/send! channel (pr-str message))))

(defn on-receive [dispatch]
  (fn [channel message]
    (-> message
        edn/read-string
        (dispatch channel))))

(defn send-message [channel message]
  (server/send! channel (pr-str message)))

(defn handler [dispatch]
  (ring/ring-handler
   (ring/router
    ["" ["/ws"
         (fn [request]
           (server/as-channel request
                              {:on-open    connect!
                               :on-receive (on-receive dispatch)
                               :on-close   disconnect!}))]])
   (ring/create-default-handler)))


(defn start [& {:keys [dispatch port]}]
  (server/run-server (handler dispatch)
                     {:port (or port 8081)})
  (println "server connected at port" port))

(comment
  (notify-clients "Everybody get on the floor.")
  )

