(ns motform.strange.material.data
  (:require [motform.strange.util :as util]))

(def panels
  (util/index-by-key
   [#:panel
    {:type          :panel/repl
     :sidebar-label "R"
     :heading       "REPL"}
    
    #:panel
    {:type          :panel/timeline
     :sidebar-label "T"
     :heading       "Timeline"}]
   :panel/type))
