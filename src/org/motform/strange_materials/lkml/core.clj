(ns org.motform.strange-materials.lkml.core
  (:require [cljfx.api          :as fx]
            [clojure.core.cache :as cache]
            [org.motform.strange-materials.lkml.ui :as ui]))

;; TODO move all this to ui namespace

(def *state
  (atom
   (fx/create-context
    {:selected-system-call nil
     :linux/command        "" 
     :linux/responses      (sorted-map)
     :email/offset         0
     :email/selected       nil}
    cache/lru-cache-factory)))

(def event-handler
  (-> ui/event-handler
      (fx/wrap-co-effects
       {:fx/context (fx/make-deref-co-effect *state)})
      (fx/wrap-effects
       {:context  (fx/make-reset-effect *state)
        :dispatch  fx/dispatch-effect
        :ssh       ui/ssh-effect})))

(def renderer
  (fx/create-renderer
   :middleware (comp fx/wrap-context-desc
                     (fx/wrap-map-desc #'ui/root))
   :opts {:fx.opt/map-event-handler event-handler
          :fx.opt/type->lifecycle #(or (fx/keyword->lifecycle %)
                                       (fx/fn->lifecycle-with-context %))}))

(defn -main [& _]
  (fx/mount-renderer *state renderer))

(comment
  (-main)
  (renderer)
  (swap! org.motform.strange-materials.lkml.core/*state identity) ; touch state

  (require 'clojure.string)

  (->> (-> @*state :cljfx.context/m :repl/responses)
       (map :val)
       reverse
       (clojure.string/join "\n"))

  (-> @*state :cljfx.context/m :linux/responses keys)

  )
