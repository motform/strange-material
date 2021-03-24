(ns motform.strange.material.core
  (:require [cljfx.api                       :as fx]
            [clojure.core.cache              :as cache]
            [motform.strange.material.events :as events]
            [motform.strange.material.views  :as views]))

(def *state
  (atom
   (fx/create-context
    {:text "foo"
     :showing? true}
    cache/lru-cache-factory)))

(def event-handler
  (-> events/event-handler
      (fx/wrap-co-effects
       {:fx/context (fx/make-deref-co-effect *state)})
      (fx/wrap-effects
       {:context (fx/make-reset-effect *state)
        :dispatch fx/dispatch-effect})))

(def renderer
  (fx/create-renderer
   :middleware (comp fx/wrap-context-desc
                     (fx/wrap-map-desc #'views/root))
   :opts {:fx.opt/map-event-handler event-handler
          :fx.opt/type->lifecycle #(or (fx/keyword->lifecycle %)
                                       (fx/fn->lifecycle-with-context %))}))

(fx/mount-renderer *state renderer)
