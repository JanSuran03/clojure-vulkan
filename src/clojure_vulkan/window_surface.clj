(ns clojure-vulkan.window-surface
  (:require [clojure-vulkan.globals :as globals :refer [VULKAN-INSTANCE WINDOW-POINTER WINDOW-SURFACE-POINTER]]
            [clojure-vulkan.util :as util])
  (:import (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK13 KHRSurface)
           (org.lwjgl.glfw GLFWVulkan)))

(defn create-surface []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [surface-ptr* (.longs stack VK13/VK_NULL_HANDLE)]
      (when (not= (GLFWVulkan/glfwCreateWindowSurface VULKAN-INSTANCE WINDOW-POINTER nil surface-ptr*)
                  VK13/VK_SUCCESS)
        (throw (RuntimeException. "Failed to create window surface.")))
      (globals/set-global! WINDOW-SURFACE-POINTER (.get surface-ptr* 0)))))

(defn destroy-surface []
  (KHRSurface/vkDestroySurfaceKHR VULKAN-INSTANCE WINDOW-SURFACE-POINTER nil)
  (globals/reset-window-surface))