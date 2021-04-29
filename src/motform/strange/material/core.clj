(ns motform.strange.material.core
  (:require [cljfx.api                       :as fx]
            [clojure.core.cache              :as cache]
            [motform.strange.material.editor :as editor]
            [motform.strange.material.kernel :as kernel]
            [motform.strange.material.events :as events]
            [motform.strange.material.views  :as views]))

(def *state
  (atom
   (fx/create-context
    {:panel/active :panel/kernel

     :kernel/selected-system-call nil
     :kernel.linux/command        "" 
     :kernel.linux/responses      (sorted-map)
     :kernel.email/offset         0
     :kernel.email/selected       nil

     ;; :repl/history     "(+ 1 1)"
     ;; :repl/responses   []
     }
    cache/lru-cache-factory)))

(def event-handler
  (-> events/event-handler
      (fx/wrap-co-effects
       {:fx/context (fx/make-deref-co-effect *state)})
      (fx/wrap-effects
       {:context  (fx/make-reset-effect *state)
        :dispatch  fx/dispatch-effect
        :ssh       kernel/ssh-effect
        :tcp       editor/tcp-effect})))

(def renderer
  (fx/create-renderer
   :middleware (comp fx/wrap-context-desc
                     (fx/wrap-map-desc #'views/root))
   :opts {:fx.opt/map-event-handler event-handler
          :fx.opt/type->lifecycle #(or (fx/keyword->lifecycle %)
                                       (fx/fn->lifecycle-with-context %))}))

(fx/mount-renderer *state renderer)

(comment
  (renderer)
  (swap! motform.strange.material.core/*state identity) ; touch state

  (require 'clojure.string)

  (->> (-> @*state :cljfx.context/m :repl/responses)
       (map :val)
       reverse
       (clojure.string/join "\n"))

  (-> @*state :cljfx.context/m :kernel.linux/responses keys)

  )
