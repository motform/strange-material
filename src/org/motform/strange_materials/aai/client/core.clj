(ns org.motform.strange-materials.aai.client.core
  (:require [gniazdo.core :as websocket]))

(defn on-receive [message]
  (tap> (str "CLIENT: on-receive: " message))
  (println message))

(defn send-prompt [socket prompt]
  (websocket/send-msg socket prompt))

(defn connect-socket [on-receive]
  (websocket/connect "ws://localhost:8081/ws"
    :on-receive on-receive
    :on-connect (fn [session] (tap> (str "CLIENT: Connected as: " session)))
    :on-close   (fn [status reason] (tap> (str "CLIENT: Disconnected with status: " status " due to " reason)))))

(defn -main
  "Command line interface."
  [& _]
  (let [socket (connect-socket on-receive)]
    (println "prompt me a river")
    (while true
      (let [prompt (read-line)]
        (send-prompt socket prompt)))))

(comment

  (def socket
    (websocket/connect "ws://localhost:8081/ws"
      :on-receive on-receive
      :on-connect (fn [session] (tap> (str "CLIENT: Connected as: " session)))
      :on-close   (fn [status reason] (tap> (str "CLIENT: Disconnected with status: " status " due to " reason)))))

  (websocket/send-msg socket "hello")
  (websocket/close socket 0 "client closing the connection")

  )
