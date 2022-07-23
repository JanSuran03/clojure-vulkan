(ns clojure-vulkan.util
  (:import (org.lwjgl.system MemoryUtil)
           (org.lwjgl.glfw GLFW)))

(defonce ^Long nullptr MemoryUtil/NULL)
