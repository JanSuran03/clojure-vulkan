(ns clojure-vulkan.globals
  (:import (org.lwjgl.vulkan VkInstance)))

(def ^long debug-messenger -1)
(def ^VkInstance vulkan-instance nil)