(set! *warn-on-reflection* true)

(ns clojure-vulkan.core
  (:require [clojure.edn :as edn]
            [clojure-vulkan.glfw :as glfw]
            [clojure-vulkan.globals :refer [*config* WINDOW-POINTER]]
            [clojure-vulkan.render :as render]
            [clojure-vulkan.util :as util]
            [clojure-vulkan.vulkan :as vulkan]
            [clojure-vulkan.window :as window])
  (:import (clojure_vulkan.Vulkan VulkanGlobals)
           (org.lwjgl.vulkan VK13)
           (org.lwjgl.glfw GLFW)))

(require '[clojure.reflect :as reflect])

(defn -main [& _]
  (let [config (edn/read-string (slurp "config.edn"))]
    (binding [*config* config
              util/*current-debug-filename* (when (:file-debug config) (util/debug-filename))]
      (when (:enable-validation-layers config)
        (VulkanGlobals/enableValidationLayers))
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
        (VK13/vkDeviceWaitIdle (VulkanGlobals/getLogicalDevice))

        (catch Throwable t
          (util/log "An error occured:" (.getMessage t)
                    (.printStackTrace t))
          #_(.printStackTrace t)
          (throw t))


        ;; termination
        (finally
          (util/try-all #(util/log "An error occurred in one of the major cleanup sections: " (.printStackTrace ^Throwable %))
            (window/destroy-window)
            (glfw/terminate)
            (vulkan/terminate)))))))