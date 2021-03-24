(ns motform.strange.material.views
  (:require [cljfx.api                     :as fx]
            [motform.strange.material.subs :as subs]))

(defn toolbar [{:keys [fx/context]}]
  {:fx/type :h-box
   :spacing 10
   :children [{:fx/type :label
               :text    (fx/sub-ctx context subs/text)}]})

(defn root [_]
  {:fx/type :stage
   :width   960
   :height  540
   :showing true
   :scene   {:fx/type :scene
             :root    {:fx/type :v-box
                       :padding 10
                       :spacing 10
                       :children [{:fx/type toolbar}]}}})
