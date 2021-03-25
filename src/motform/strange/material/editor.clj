(ns motform.strange.material.editor
  (:require [cljfx.api                       :as fx]
            [cljs.repl                       :as repl]
            [cljs.repl.browser               :as browser]
            [motform.strange.material.events :as events]
            [motform.strange.material.views  :as views]))


;;; SUBS

(defn repl-history [context]
  (fx/sub-val context :repl/history))

;;; EVENTS

(defmethod events/event-handler ::type-text [{:fx/keys [event context]}]
  {:context (fx/swap-context context assoc :repl/history event)})

;;; VIEWS

;; text-area might be too simple for an editor
;; as it only supports :text from a string?
(defn editor [{:keys [fx/context]}]
  {:fx/type :text-area
   :style-class ["repl"]
   :text (fx/sub-ctx context repl-history)
   :on-text-changed {:event/type ::type-text}})

(defmethod views/panel :panel/repl [_]
  {:fx/type     :v-box
   :style-class ["panel"]
   :children    [{:fx/type :label
                  :text   "REPL!"}
                 {:fx/type editor}]})


(comment

  (def env (browser/repl-env))

  (repl/repl env)
  )

(defmethod views/panel :panel/repl [_]
  {:fx/type     :v-box
   :style-class ["panel"]
   :children    [{:fx/type :label
                  :text   "REPL!"}
                 {:fx/type editor}]})
