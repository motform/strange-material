(ns org.motform.strange-materials.aai.server.open-ai
  "Wrapper around the OpenAI API beta. Bring your own API key!"
  (:require [org.httpkit.client :as client]
            [clojure.data.json  :as json]
            [clojure.set        :as set]
            [org.motform.strange-materials.config :refer [config]]))

(def ^:private api-key (get-in config [:open-ai :api-key]))

(def valid-engines
  #{:content-filter-alpha-c4 :content-filter-dev :cursing-filter-v6
    :ada
    :babbage
    :curie   :curie-instruct-beta
    :davinci :davinci-instruct-beta})

(def valid-params
  #{:logit_bias :frequency_penalty :presence_penalty
    :stop       :echo              :logprobs
    :stream     :n                 :best_of
    :top_p      :temperature       :max_tokens
    :prompt})

(def param-defaults
  {:max_tokens  64
   :temperature 0.7
   :top_p       1})

(defn parse-json [json]
  (json/read-str json :key-fn #(keyword "open-ai" %)))

(defn- request [engine task params]
  (client/post (str "https://api.openai.com/v1/engines/" engine "/" task)
               {:headers {"content-type" "application/json"}
                :basic-auth   ["" api-key]
                :body         (json/write-str (merge param-defaults params))}))

(defn completion-with
  {:style/indent 1}
  [engine params]
  {:pre [(valid-engines engine)
         (set/subset? (set (keys params)) valid-params)]}
  (request (name engine) "completions" params))

(defn response-body [response]
  (-> response :body parse-json))

(defn response-text [response]
  (-> response :body parse-json :open-ai/choices first :open-ai/text))


