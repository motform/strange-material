(ns motform.strange.material.sidebar
  (:require [cljfx.api                       :as fx]
            [motform.strange.material.data   :as data]
            [motform.strange.material.events :as events]
            [motform.strange.material.subs   :as subs]))

(defmethod events/event-handler :panel/select [{:keys [value fx/context]}]
  {:context (fx/swap-context context assoc :panel/active value)})

(defn sidebar [{:keys [fx/context]}]
  (let [active-panel (fx/sub-ctx context subs/active-panel)]
    {:fx/type     :v-box
     :min-width   1000
     :style-class ["sidebar"]
     :children    (for [[_ {:panel/keys [type sidebar-label]}] data/panels]
                    {:fx/type     :button
                     :text        sidebar-label
                     :on-action   {:event/type :panel/select :value type}
                     :style-class ["sidebar-panel-indicator"
                                   (when (= active-panel type) "sidebar-panel-indicator-active")]})}))

