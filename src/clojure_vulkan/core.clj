(set! *warn-on-reflection* true)

(ns clojure-vulkan.core
  (:require [clojure-vulkan.window :as window]
            [clojure-vulkan.vulkan :as vulkan]
            [clojure-vulkan.glfw :as glfw]
            [clojure-vulkan.util :as util]
            [clojure-vulkan.validation-layers :as validation-layers]))

(defn find-or-default [opts key]
  (if-let [key (find opts key)]
    (get opts key)
    true))

(defn -main [& [opts]]
  (binding [util/*doto-debug* (find-or-default opts :debug)
            validation-layers/*enable-validation-layers* (find-or-default opts :validation)]
    (try
      ;; init
      (glfw/init)
      (vulkan/init)
      (window/create-window)
      (window/show-window)

      ;; application loop
      (while (not (window/should-window-close?))
        (glfw/poll-events))

      (catch Throwable t
        (println "An error occured:" (.getMessage t))
        (.printStackTrace t))


      ;; termination
      (finally
        (window/destroy-window)
        (glfw/terminate)
        (vulkan/terminate)))))