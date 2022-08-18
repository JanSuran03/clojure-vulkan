(set! *warn-on-reflection* true)

(ns clojure-vulkan.core
  (:require [clojure-vulkan.glfw :as glfw]
            [clojure-vulkan.globals :refer [LOGICAL-DEVICE WINDOW-POINTER]]
            [clojure-vulkan.render :as render]
            [clojure-vulkan.util :as util]
            [clojure-vulkan.validation-layers :as validation-layers]
            [clojure-vulkan.vulkan :as vulkan]
            [clojure-vulkan.window :as window])
  (:import (org.lwjgl.vulkan VK13)
           (org.lwjgl.glfw GLFW)))

(defn find-or-default [opts key]
  (if-let [key (find opts key)]
    (get opts key)
    true))

(list :file-debug)

(defn -main [& [{:keys [file-debug] :as opts}]]
  (binding [validation-layers/*enable-validation-layers* (find-or-default opts :validation)
            util/*current-debug-filename* (when file-debug (util/debug-filename))]
    (try
      ;; init
      (glfw/init)
      (window/create-window)
      (vulkan/init)
      (GLFW/glfwShowWindow WINDOW-POINTER)

      ;; application loop
      (while (not (window/should-window-close?))
        (glfw/poll-events)
        (render/draw-frame))
      (VK13/vkDeviceWaitIdle LOGICAL-DEVICE)

      (catch Throwable t
        (println "An error occured:" (.getMessage t)
                 (.printStackTrace t))
        #_(.printStackTrace t)
        (throw t))


      ;; termination
      (finally
        (window/destroy-window)
        (glfw/terminate)
        (vulkan/terminate)))))