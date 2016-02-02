(require 'cljs.build.api)

(def source-dir "src/main/clojurescript")

(defn call [api-f] 
  (api-f
    source-dir
    {:main
     'dmos.svarus.core
     :optimizations
     :none
     :output-dir
     "${project.build.directory}/${project.build.finalName}/js"
     :output-to
     "${project.build.directory}/${project.build.finalName}/js/main.js"
     :source-map
     true
    ;:source-map 
    ;"${project.build.directory}/${project.build.finalName}/js/main.js.map"
     :asset-path
     "/js"
     }))

(defn complain [& ss] (throw (Exception. (apply str ss))))

(defn production []
  (cljs.build.api/build
    source-dir
    {
     :optimizations
     :advanced
     :output-to
     "${project.build.directory}/${project.build.finalName}/js/main.js"
     }))

(case (first *command-line-args*)
  "build" (call cljs.build.api/build)
  "watch" (call cljs.build.api/watch)
  "prod" (production)
  nil
  (complain
    (str
      "You should tell me what to do. "
      "Valid options are watch, build, prod.")))

(System/exit 0)
