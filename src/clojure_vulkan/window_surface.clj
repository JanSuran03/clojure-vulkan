(ns clojure-vulkan.window-surface
  (:require [clojure-vulkan.globals :refer [vulkan-instance window-ptr window-surface-ptr]]
            [clojure-vulkan.util :as util])
  (:import (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK13)
           (org.lwjgl.glfw GLFWVulkan)))

(defn create-surface []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [surface-ptr* (.longs stack VK13/VK_NULL_HANDLE)]
      (when (not= (GLFWVulkan/glfwCreateWindowSurface vulkan-instance window-ptr nil surface-ptr*)
                  VK13/VK_SUCCESS)
        (throw (RuntimeException. "Failed to create window surface.")))
      (alter-var-root #'window-surface-ptr (constantly (.get surface-ptr* 0))))))