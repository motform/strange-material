(ns org.motform.strange-materials.aai.server
  (:require [clojure.string     :as str]
            [mount.core         :as mount :refer [defstate]]
            [org.httpkit.server :as server]
            [reitit.ring        :as ring]
            [org.motform.strange-materials.util        :as util]
            [org.motform.strange-materials.aai.open-ai :as open-ai]))

(set! *warn-on-reflection* true)

(defonce channels (atom #{}))

(defn- connect! [channel]
  (tap> "SERVER channel open")
  (swap! channels conj channel))

(defn- disconnect! [channel status]
  (tap> (str "SERVER channel closed: " status))
  (swap! channels #(remove #{channel} %)))

(defn- notify-clients [message]
  (doseq [channel @channels]
    (server/send! channel message)))

(defn- notify-other-clients [excluded-channel message]
  (doseq [channel (remove #{excluded-channel} @channels)]
    (server/send! channel message)))

(defn- completion [prompt]
  (-> (open-ai/completion-with :davinci-instruct-beta
        {:prompt prompt :max_tokens 200})
      util/realize
      open-ai/response-text
      str/triml))

(defn- on-receive [channel message]
  (tap> (str "SERVER on-receive: " message))
  (let [interception (str message (util/prompt "intercept"))
        [clean dirty] (pvalues (completion message) (completion interception))]
    (server/send! channel clean)
    (notify-other-clients channel dirty)))

(def websocket-routes
  ["/ws" (fn [request]
           (server/as-channel request
                              {:on-open    connect!
                               :on-receive on-receive
                               :on-close   disconnect!}))])

(def handler
  (ring/ring-handler
   (ring/router
    ["" [websocket-routes]])))

(defstate server
  :start (do (tap> "SERVER starting")
             (server/run-server #'handler {:port 8081})
             (println "server running"))
  :stop (when server (server :timeout 100)))

(defn -main [& _]
  (mount/start))

(comment
  (mount/start)
  (mount/stop)
  
  (notify-clients "Everybody get on the floor.")
  )
