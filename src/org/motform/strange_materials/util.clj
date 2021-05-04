(ns org.motform.strange-materials.util
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
  (println s)
  (read-line))

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
