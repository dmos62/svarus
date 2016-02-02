(ns dmos.cljs.trace)

(defn forms-to-trace [forms]
  (let [random-id (str (rand-int 999))
        spaces (fn [n] (apply str (take n (repeat " "))))
        base-offset (+ 1 3)
        inner-padding (spaces (- base-offset (count random-id)))
        line-offset (spaces (+ base-offset (count inner-padding)))
        lines (partition-all 2 forms)
        margins (loop [margins (list) count-left (dec (count lines))]
                  (cond
                    (= 1 (count lines)) (list (str random-id " " inner-padding))
                    (empty? margins) (recur (conj margins
                                                  (str ">" random-id
                                                       inner-padding))
                                            (dec count-left))
                    (zero? count-left) (conj margins
                                             (str random-id "<"
                                                  inner-padding))
                    true (recur (conj margins line-offset)
                                (dec count-left))))
        formatted (->> (map #(apply str (list (first %) ": "
                                              (pr-str (second %)) "\n")) lines)
                       (interleave margins)
                       (apply str))]
    formatted))

(defn print-to-user [& ss]
  (let [s (str (apply str (interpose " " ss)) "\n")]
    #?( :clj
        (.write *out* s)
        :cljs
        (println s))))

(defn TRACE [& forms] 
  (print-to-user (forms-to-trace forms))
  (last forms))

(def ^:dynamic *trace-explicitly-enabled* false)

(defn TRACE-2 [& forms]
  (when *trace-explicitly-enabled*
    (print-to-user (forms-to-trace forms)))
  (last forms))
