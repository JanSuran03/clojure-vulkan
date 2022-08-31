(ns clojure-vulkan.window-surface
  (:require [clojure-vulkan.globals :refer [WINDOW-POINTER]]
            [clojure-vulkan.util :as util])
  (:import (clojure_vulkan.Vulkan VulkanGlobals)
           (org.lwjgl.glfw GLFWVulkan)
           (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK13 )))

(defn create-surface []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [surface-ptr* (.longs stack VK13/VK_NULL_HANDLE)]
      (when (not= (GLFWVulkan/glfwCreateWindowSurface (.get VulkanGlobals/VULKAN_INSTANCE) WINDOW-POINTER nil surface-ptr*)
                  VK13/VK_SUCCESS)
        (throw (RuntimeException. "Failed to create window surface.")))
      (.set VulkanGlobals/WINDOW_SURFACE_POINTER (.get surface-ptr* 0)))))
