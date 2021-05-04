(ns org.motform.strange-materials.aai.client.core
  (:require [gniazdo.core :as websocket]))

(defn on-receive [message]
  (tap> (str "CLIENT: on-receive: " message))
  (println message))

(def socket
  (websocket/connect "ws://localhost:8081/ws"
    :on-receive on-receive
    :on-connect (fn [session] (tap> (str "CLIENT: Connected as: " session)))
    :on-close   (fn [status reason] (tap> (str "CLIENT: Disconnected with status: " status " due to " reason)))))

(defn send-prompt [prompt]
  (websocket/send-msg socket prompt))

(defn -main [& _]
  (println "prompt me a river")
  (while true
    (let [prompt (read-line)]
      (send-prompt prompt))))

(comment
  (websocket/send-msg socket "hello")
  (websocket/close socket 0 "client closing the connection")
  )
