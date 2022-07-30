(ns clojure-vulkan.swap-chain
  (:require [clojure-vulkan.globals :refer [window-surface-ptr]]
            [clojure-vulkan.util :as util])
  (:import (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VkPhysicalDevice VkSurfaceCapabilitiesKHR VK13 KHRSurface VkSurfaceFormatKHR VkSurfaceFormatKHR$Buffer)))

(defn query-swap-chain-support [^VkPhysicalDevice device]
  (util/with-memory-stack-push ^MemoryStack stack
    (let [^VkSurfaceCapabilitiesKHR capabilities (VkSurfaceCapabilitiesKHR/malloc stack)
          _ (KHRSurface/vkGetPhysicalDeviceSurfaceCapabilitiesKHR device window-surface-ptr capabilities)
          format-count-ptr (.ints stack 0)
          _ (KHRSurface/vkGetPhysicalDeviceSurfaceFormatsKHR device window-surface-ptr format-count-ptr nil)
          format-count (.get format-count-ptr 0)
          formats (when-not (zero? format-count)
                    (let [formats-ptr (VkSurfaceFormatKHR/malloc format-count stack)]
                      (KHRSurface/vkGetPhysicalDeviceSurfaceFormatsKHR device window-surface-ptr format-count-ptr formats-ptr)
                      formats-ptr))
          present-mode-count-ptr (.ints stack 0)
          _ (KHRSurface/vkGetPhysicalDeviceSurfacePresentModesKHR device window-surface-ptr present-mode-count-ptr nil)
          present-mode-count (.get present-mode-count-ptr 0)
          present-modes-ptr (when-not (zero? present-mode-count)
                              (let [present-modes-ptr (.mallocInt stack present-mode-count)]
                                (KHRSurface/vkGetPhysicalDeviceSurfacePresentModesKHR device window-surface-ptr present-mode-count-ptr present-modes-ptr)
                                present-modes-ptr))]
      (and formats present-modes-ptr
           {:formats-ptr       formats
            :present-modes-ptr present-modes-ptr
            :capabilities      capabilities}))))

(defn choose-swap-surface-format [^VkSurfaceFormatKHR$Buffer formats]
  (or (->> formats
           util/buffer->seq
           (some (fn [^VkSurfaceFormatKHR format]
                   (and (= (.format format) VK13/VK_FORMAT_B8G8R8_UNORM)
                        (= (.colorSpace format) KHRSurface/VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                        format))))
      (.get formats 0)))