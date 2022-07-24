(ns clojure-vulkan.util
  (:import (org.lwjgl.system MemoryUtil MemoryStack)
           (org.lwjgl.glfw GLFW)
           (org.lwjgl.vulkan VK13)))

(defonce ^Long nullptr MemoryUtil/NULL)

(defmacro with-memory-stack [stack & body]
  `(with-open [~stack (MemoryStack/stackPush)]
     ~@body))

(def version-major 1)
(def version-minor 3)
(def vk-version (VK13/VK_MAKE_VERSION version-major version-minor 0))