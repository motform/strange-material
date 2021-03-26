(ns motform.strange.material.views
  (:require [cljfx.api                        :as fx]
            [cljfx.css                        :as css]
            [motform.strange.material.data    :as data]
            [motform.strange.material.sidebar :refer [sidebar]]
            [motform.strange.material.styles  :as styles]
            [motform.strange.material.subs    :as subs]))

(defmulti panel :panel/type)

(defmethod panel :default [_]
  {:fx/type     :label
   :style-class ["panel"]
   :text        "Default panel."})

(defmethod panel :panel/timeline [_]
  {:fx/type     :label
   :style-class ["panel" "timeline"]
   :text        "Timeline!"})

(defn main-panel [{:keys [fx/context]}]
  (-> (fx/sub-ctx context subs/active-panel)
      data/panels
      panel))

(defn root [_]
  {:fx/type :stage
   :width   960
   :height  540
   :showing true
   :title   "Visus"
   :scene   {:fx/type :scene
             :stylesheets [(::css/url styles/styles)]
             :root        {:fx/type :h-box
                           :children [{:fx/type sidebar}
                                      {:fx/type main-panel}]}}})
