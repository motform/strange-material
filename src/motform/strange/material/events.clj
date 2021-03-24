(ns motform.strange.material.events
  (:require [cljfx.api :as fx]))

(defmulti event-handler :event/type)

(defmethod event-handler :default [event]
  (prn event))
