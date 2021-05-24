(ns org.motform.strange-materials.util
  "Miscellaneous utility functions."
  (:require [clojure.string :as str]))

(defn distinct-by-key
  "Filters `ms` so to be distinct by `k`."
  [ms k]
  (->> ms
       (reduce (fn [{:keys [seen] :as state} {k k :as m}]
                 (if-not (seen k)
                   (-> state (update :acc conj m) (update :seen conj k))
                   state))
               {:seen #{} :acc []})
       :acc))

(defn realize
  "Blocks main thread while trying to realize `x`, sorry about that."
  [x]
  (while (not (realized? x)))
  @x)

(defn empty-view
  "Dirty 'hack' to get a nil-view."
  [_]
  {:fx/type :label
   :text    ""})

(defn random-port []
  (+ 8000 (rand-int 1000)))

(defn break-lines
  "Add \n in `s` at every `i`."
  [s i]
  (if (str/blank? s)
    ""
    (->> (str/escape s {\newline ""})
         (partition-all i)
         (interpose "\n")
         (map (partial apply str))
         (apply str))))
