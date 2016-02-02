(ns dmos.cljs.http-client
  (:require [cljs.core.async
             :refer [close! take! chan put! <! >! to-chan]]
            [goog.net.XhrIo :as XhrIo]))

(defn <req [{:keys [url method body headers before-f after-f] :as params}]
  "XhrIo wrap."
  (let [c (chan)
        js-headers (apply js-obj (reduce concat (seq headers)))
        cb
        (fn [e]
          (let [xhrio (.-target e)
                resp {:body (.getResponseText xhrio)
                      :status (.getStatus xhrio)
                      :date (.getResponseHeader xhrio "date")}]
            ;(go (>! c resp)
            ;    (close! c))
            (put! c resp)
            (close! c)
            (when after-f
              (after-f (dissoc resp :body :before-f :after-f)))
            ))]
    (when before-f (before-f (dissoc params :body)))
    (do (XhrIo/send url cb method body js-headers) c)))
