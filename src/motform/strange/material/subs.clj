(ns motform.strange.material.subs
  (:require [cljfx.api :as fx]))

(defn text [context]
  (fx/sub-val context :text))
