(ns org.motform.strange-materials.util
  (:require [clojure.string :as str])
  (:import [javafx.stage Screen Window]))

(defn index-by-key [ms idx-k]
  (into {} (map (juxt idx-k identity)) ms))

(defn distinct-by-key [ms k]
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

(defn prompt [s]
  (print s)
  (flush)
  (read-line))

(defn remove-period [s]
  (str/replace s #"\." ""))

(defn screen-height []
  (-> (Screen/getPrimary) .getBounds .getHeight))

(defn screen-width []
  (-> (Screen/getPrimary) .getBounds .getWidth))

(defn window-height
  "Assumes that there is a single window open"
  []
  (-> (Window/getWindows) first .getHeight))

(defn window-width
  "Assumes that there is a single window open"
  []
  (-> (Window/getWindows) first .getWidth))

(defn empty-view
  "Dirty 'hack' to get a nil-view."
  [_]
  {:fx/type :label
   :text    ""})

(defn random-port []
  (+ 8000 (rand-int 1000)))

(defn break-lines [s i]
  (if (str/blank? s)
    ""
    (->> (str/escape s {\newline ""})
         (partition-all i)
         (interpose "\n")
         (map (partial apply str))
         (apply str))))
