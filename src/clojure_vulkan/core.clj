(set! *warn-on-reflection* true)

(ns clojure-vulkan.core
  (:require [clojure-vulkan.glfw :as glfw]
            [clojure-vulkan.util :as util]
            [clojure-vulkan.validation-layers :as validation-layers]
            [clojure-vulkan.vulkan :as vulkan]
            [clojure-vulkan.window :as window]))

(defn find-or-default [opts key]
  (if-let [key (find opts key)]
    (get opts key)
    true))

(list :file-debug)

(defn -main [& [{:keys [file-debug] :as opts}]]
  (binding [util/*doto-debug* (find-or-default opts :debug)
            validation-layers/*enable-validation-layers* (find-or-default opts :validation)
            util/*current-debug-filename* (when file-debug (util/debug-filename))]
    (try
      ;; init
      (glfw/init)
      (window/create-window)
      (vulkan/init)
      (window/show-window)

      ;; application loop
      (while (not (window/should-window-close?))
        (glfw/poll-events))

      (catch Throwable t
        (println "An error occured:" (.getMessage t))
        #_(.printStackTrace t)
        (throw t))


      ;; termination
      (finally
        (window/destroy-window)
        (glfw/terminate)
        (vulkan/terminate)))))