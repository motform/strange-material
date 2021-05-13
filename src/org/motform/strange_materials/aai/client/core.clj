(ns org.motform.strange-materials.aai.client.core
  (:require [gniazdo.core :as websocket]))

(defn on-receive [message]
  (tap> (str "CLIENT: on-receive: " message))
  (println "CLIENT: on-receive: " message))

(defn send-message
  "The shape of the message map:
  #:message
  {:type #:message[prompt|handshake]
   :from #:client{id #uuid name str}
   :body str}"
  [socket message]
  (println message)
  (websocket/send-msg socket (pr-str message)))

(defn connect-socket [port on-receive]
  (websocket/connect (str "ws://localhost:" port "/ws")
    :on-receive on-receive
    :on-connect (fn [session] (tap> (str "CLIENT: Connected as: " session)))
    :on-close   (fn [status reason] (tap> (str "CLIENT: Disconnected with status: " status " due to " reason)))))

(defn -main
  "Command line interface."
  [& {:keys [port]}]
  (let [socket (connect-socket port on-receive)]
    (println "prompt me a river")
    (while true
      (let [prompt (read-line)]
        (send-message socket (pr-str prompt))))))

(comment

  (def socket
    (websocket/connect "ws://localhost:8494/ws"
      :on-receive on-receive
      :on-connect (fn [session] (tap> (str "CLIENT: Connected as: " session)))
      :on-close   (fn [status reason] (tap> (str "CLIENT: Disconnected with status: " status " due to " reason)))))

  (websocket/send-msg socket "hello")
  (websocket/close socket 0 "client closing the connection")

  )
