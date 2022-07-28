(ns clojure-vulkan.globals
  (:import (org.lwjgl.vulkan VkInstance VkPhysicalDevice VK13)))

(def ^long debug-messenger VK13/VK_NULL_HANDLE)
(def ^VkInstance vulkan-instance VK13/VK_NULL_HANDLE)
(def ^VkPhysicalDevice physical-device VK13/VK_NULL_HANDLE)