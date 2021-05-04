(ns org.motform.strange-materials.config
  (:require [clojure.edn :as edn]))

(def config-path "resources/edn/config.edn")

(def config
  (-> config-path
      slurp
      edn/read-string))
