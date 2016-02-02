(ns dmos.cljs.navigation
  (:require ;[dmos.raudondvaris.route-match :refer [match-uri]]
            [dmos.cljs.trace :refer [TRACE]]
            [clojure.string :as str]
            [goog.events]
            [goog.history.Html5History]
            [goog.history.EventType]
            [goog.Uri]))

; (defn match-token [pattern token]
;   (match-uri pattern token))

; (def app-prefix (:prefix @(get-token-info-cursor)))

; (defn app-path [sub-path] (str app-prefix sub-path))

(defn set-app-token! [token-atom path]
  ;(let [token (subs path (count app-prefix))]
  (->>
    (str/split path (re-pattern "/"))
    (remove empty?)
    vec
    (reset! token-atom)))

(defonce history
  (doto
    (goog.history.Html5History.)
    (.setUseFragment false)
    (.setEnabled true)
    (.setPathPrefix "")))

(defn set-token! [token] (.setToken history token))

(defn navigate-in-app! [e]
  (.preventDefault e)
  (let [path (.getPath (goog.Uri. (.. e -target -href)))]
    (.preventDefault e)
    ; kodel sitas cia buvo?
    ; (set-app-token! path)
    (set-token! path)))

(defn handle-navigate! [token-atom e]
  (when (.-isNavigation e) 
    (set-app-token! token-atom (.-token e))))

(defn init-navigation! [token-atom]
  (set-app-token! token-atom (.. js/window -location -pathname))
  (goog.events/listen
    history goog.history.EventType/NAVIGATE
    (partial handle-navigate! token-atom)))

; (defn internal-link [{:keys [text sub-path]} owner]
;   (reify
;     om/IRender
;     (render [_]
;       (let [path (app-path sub-path)]
;         (dom/span
;           (str text ": ")
;           (dom/a {:href path :on-click navigate-in-app!} path))))))
