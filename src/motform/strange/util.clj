(ns motform.strange.util)

(defn index-by-key
  "Index maps in `ms` by `idx-k`. Assumes all maps associate `idx-k`."
  [ms idx-k]
  (reduce
   (fn [acc m]
     (assoc acc (get m idx-k) m))
   {}
   ms))
