(ns dmos.svarus.core
  (:require [cljs.core.async
             :refer [close! take! chan put! <! >! to-chan]
             :as async]
            [clojure.string :as str]
           ;[dmos.raudondvaris.trace :refer [TRACE]]
           ;[dmos.raudondvaris.http-client :refer [<req]]
           ;[dmos.raudondvaris.navigation
           ; :refer [navigate-in-app! set-app-token! match-token
           ;         internal-link]]
            [dmos.cljs.trace :refer [TRACE]]
            [goog.events]
            [goog.events.EventType]
            [reagent.core :as r]
            [dmos.cljs.navigation :as n]
            [cljs.core.match :refer-macros [match]]
            [dmos.svarus.filmai :refer [filmai]]
            )
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(enable-console-print!)

(defn router [token-atom]
  (match [@token-atom]
         [[]] [:h1 "tu namai"]
         [["filmai"]] [filmai]
         :else [:p "404"]
         ))

(def token (r/atom []))

(add-watch
  token nil
  (fn [_ _ o n]
    ;(TRACE "token" n)
    ))

(n/init-navigation! token)

(defn main []
  (r/render
    [router token]
    (.getElementById js/document "container")))

(goog.events/listen
  js/document
  goog.events.EventType/DOMCONTENTLOADED
  main)
