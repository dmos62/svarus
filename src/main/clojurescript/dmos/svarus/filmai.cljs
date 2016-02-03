(ns dmos.svarus.filmai
  (:require [cljs.core.async
             :refer [map< close! take! chan put! <! >! to-chan]
             :as async]
            [clojure.string :as str]
            [dmos.cljs.trace :refer [TRACE]]
            [goog.events]
            [goog.events.EventType]
            [reagent.core :as r]
            [dmos.cljs.http-client :refer [<req]]
            [cljs.core.match :refer-macros [match]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(def local-state (r/atom nil))

(def remote-state (r/atom nil))

(defn <parsiusk-sarasa []
  (map< :body (<req {:url "/public/filmai" :method "GET"})))

(defn <ikelk-sarasa! []
  (map< :body (<req {:url "/xxx/filmai" :method "PUT" :body @local-state})))

(defn decorate [block]
  [:div
   {:class "block"}
   (when-let [h (:header block)] [:h2 h])
   (map #(vector :div %) (:lines block))])

(defn parse-header [line]
  (second (str/split line #"^#+\s*")))

(defn parse [lines]
  (if-let [header (parse-header (first lines))]
    {:header header :lines (rest lines)}
    {:lines lines}
    ))

(defn to-blocks [delimiter-p plain]
  (let [lines (->> (str/split-lines plain) (remove str/blank?) (map str/trim))]
    (loop [blocks-acc [] block [(first lines)] lines (rest lines)]
      (let [line (first lines)]
        (cond
          (nil? line) 
          (conj blocks-acc block)
          (delimiter-p line)
          (recur (conj blocks-acc block) [line] (rest lines))
          true
          (recur blocks-acc (conj block line) (rest lines))
          )))))

(defn pakeistas? []
  (not= @local-state @remote-state))

(def no-action-href "javascript:void(0)")

(defn action [text f]
  [:a {:href no-action-href :on-click f} text])

(def editing? (r/atom false))

(defn valdymas [admin?]
  (when @admin?
    (let [editing?1 @editing?
          pakeistas?1 (pakeistas?)]
      [:div
       {:id "valdymas"}
       [action (if editing?1 "neberedaguoti" "redaguoti") #(swap! editing? not)]
       (when pakeistas?1
         (list 
           [action "ikelti"
            #(take! (<ikelk-sarasa!) (fn [n] (reset! remote-state n)))]
           [action "atstatyti" #(reset! local-state @remote-state)]
           ;[action "nuimk;gala" #(swap! local-state (fn [o] (subs o 1)))]
           ))
       (when (and (not pakeistas?1) editing?1)
         "nera pakeitimu")
       ])))

(defn set-states! [n]
  (reset! remote-state n)
  (reset! local-state n))

(defn filmai [admin?]
  (take!
    (<parsiusk-sarasa)
    set-states!)
  (fn []
    [:div
     {:id "filmai"}
     [:div {:id "render"}
      (->>
        @remote-state
        (to-blocks parse-header)
        (map parse)
        (map decorate)
        )]
     (when @admin?
       [:div {:id "admino"}
        [valdymas admin?]
        (when @editing?
          (list 
            [:textarea
             {:on-change #(reset! local-state (.-target.value %))
              :value @local-state}]
            [valdymas admin?]
            ))])]))

(add-watch
  local-state nil
  (fn [_ _ o n]
    ;(TRACE "local-state" n)
    ))
