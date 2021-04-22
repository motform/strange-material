(ns motform.strange.util)

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


