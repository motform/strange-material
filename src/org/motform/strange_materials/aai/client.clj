(ns org.motform.strange-materials.aai.client
  (:require [gniazdo.core :as websocket]
            [mount.core   :as mount :refer [defstate]]))

(def socket
  (websocket/connect "ws://localhost:8081/ws"
                     :on-receive #(prn 'recived %)))

(comment
  (websocket/send-msg socket "hello")
  (websocket/close socket)
  )
