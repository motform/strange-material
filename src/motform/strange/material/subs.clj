(ns motform.strange.material.subs
  (:require [cljfx.api :as fx]))

(defn active-panel [context]
  (fx/sub-val context :panel/active))

