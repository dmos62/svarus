(ns dmos.svarus.filmai
  (:require [cljs.core.async
             :refer [close! take! chan put! <! >! to-chan]
             :as async]
            [clojure.string :as str]
            [dmos.cljs.trace :refer [TRACE]]
            [goog.events]
            [goog.events.EventType]
            [reagent.core :as r]
            [dmos.cljs.http-client :refer [<req]]
            [cljs.core.match :refer-macros [match]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(def filmai-atom
  (r/atom
    "# Rugpjutis 2010
    Kokaksdk (1230)
    Kokaksdk (1230)
    # Rugpjutis 2011
    Kokaksdk (1230)
    Kokaksdk (1230)
    # Rugpjutis 2012
    Kokaksdk (1230)
    Kokaksdk (1230)"))

(defn parsiusk-sarasa! []
  (go 
    (when-let [r (<! (<req {:url "/public/filmai" :method "GET"}))]
      (reset! filmai-atom (:body r))
      )))

(defn filmai []
  (parsiusk-sarasa!)
  (fn []
    [:div @filmai-atom]
    ))
