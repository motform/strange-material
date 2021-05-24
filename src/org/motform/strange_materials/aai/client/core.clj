(ns org.motform.strange-materials.aai.client.core
  "Websocket client functionality."
  (:require [gniazdo.core :as websocket]))

(defn send-message
  "The shape of the message map:
  #:message
  {:body str
   :headers {:client/name  str
             :client/id    uuid
             :message/type enum}}"
  [socket message]
  (websocket/send-msg socket (pr-str message)))

(defn connect-socket [port host on-receive]
  (websocket/connect (str "ws://" host ":" port "/ws")
                     :on-receive on-receive
                     :on-connect (fn [session] (tap> (str "CLIENT: Connected as: " session)))
                     :on-close   (fn [status reason] (tap> (str "CLIENT: Disconnected with status: " status " due to " reason)))))

(defn- on-receive-cli [message]
  (tap> (str "CLIENT: on-receive: " message)))

(comment

  (def socket
    (websocket/connect "ws://localhost:8494/ws"
                       :on-receive on-receive-cli
                       :on-connect (fn [session] (tap> (str "CLIENT: Connected as: " session)))
                       :on-close   (fn [status reason] (tap> (str "CLIENT: Disconnected with status: " status " due to " reason)))))

  (websocket/send-msg socket "hello")
  (websocket/close socket 0 "client closing the connection")

  )
