(ns org.motform.strange-materials.config
  "Simple configuration ns primarily used to hide secrets.
  Expects an .edn at config-path to contain {:open-ai {:api-key KEY}}."
  (:require [clojure.edn :as edn]))

(def config-path "resources/edn/config.edn")

(def config
  (-> config-path
      slurp
      edn/read-string))
