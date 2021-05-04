(ns org.motform.strange-materials.aai.server
  (:require [mount.core         :as mount :refer [defstate]]
            [org.httpkit.server :as server]
            [reitit.ring        :as ring]))

(set! *warn-on-reflection* true)

(defonce channels (atom #{}))

(defn connect! [channel]
  (tap> "SERVER: channel open")
  (swap! channels conj channel))

(defn disconnect! [channel status]
  (tap> (str "SERVER: channel closed: " status))
  (swap! channels #(remove #{channel} %)))

(defn notify-clients [msg]
  (doseq [channel @channels]
    (server/send! channel msg)))

(def websocket-routes
  ["/ws" (fn [request]
           (server/as-channel request
                              {:on-open     connect!
                               :on-receive  (fn [ch message] (tap> (str "SERVER: on-receive: " message)))
                               :on-close    disconnect!}))])

(def handler
  (ring/ring-handler
   (ring/router
    [""
     [websocket-routes]])))

(defstate server
  :start (do (tap> "starting server")
             (server/run-server #'handler {:port 8081}))
  :stop (when server (server :timeout 100)))

(comment  
  (mount/start)
  (mount/stop)
  
  )
