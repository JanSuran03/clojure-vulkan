(ns clojure-vulkan.swap-chain
  (:require [clojure-vulkan.globals :refer [window-surface-ptr]]
            [clojure-vulkan.util :as util])
  (:import (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VkPhysicalDevice VkSurfaceCapabilitiesKHR VK13 KHRSurface VkSurfaceFormatKHR VkSurfaceFormatKHR$Buffer KHRSwapchain)
           (java.nio IntBuffer)))

(defn query-swap-chain-support [^VkPhysicalDevice device]
  (util/with-memory-stack-push ^MemoryStack stack
    (let [^VkSurfaceCapabilitiesKHR capabilities (VkSurfaceCapabilitiesKHR/malloc stack)
          _ (KHRSurface/vkGetPhysicalDeviceSurfaceCapabilitiesKHR device window-surface-ptr capabilities)
          formats-count-ptr (.ints stack 0)
          _ (KHRSurface/vkGetPhysicalDeviceSurfaceFormatsKHR device window-surface-ptr formats-count-ptr nil)
          formats-count (.get formats-count-ptr 0)
          formats (when-not (zero? formats-count)
                    (let [formats-ptr (VkSurfaceFormatKHR/malloc formats-count stack)]
                      (KHRSurface/vkGetPhysicalDeviceSurfaceFormatsKHR device window-surface-ptr formats-count-ptr formats-ptr)
                      formats-ptr))
          present-mode-count-ptr (.ints stack 0)
          _ (KHRSurface/vkGetPhysicalDeviceSurfacePresentModesKHR device window-surface-ptr present-mode-count-ptr nil)
          present-modes-count (.get present-mode-count-ptr 0)
          present-modes-ptr (when-not (zero? present-modes-count)
                              (let [present-modes-ptr (.mallocInt stack present-modes-count)]
                                (KHRSurface/vkGetPhysicalDeviceSurfacePresentModesKHR device window-surface-ptr present-mode-count-ptr present-modes-ptr)
                                present-modes-ptr))]
      (and formats present-modes-ptr
           {:formats-ptr         formats
            :present-modes-ptr   present-modes-ptr
            :present-modes-count present-modes-count
            :capabilities        capabilities}))))

(defn choose-swap-surface-format [^VkSurfaceFormatKHR$Buffer formats]
  (or (->> formats
           util/buffer->seq
           (some (fn [^VkSurfaceFormatKHR format]
                   (and (= (.format format) VK13/VK_FORMAT_B8G8R8_UNORM)
                        (= (.colorSpace format) KHRSurface/VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                        format))))
      (.get formats 0)))

(defn choose-swap-presentation-mode
  "KHRSurface/VK_PRESENT_MODE_IMMEDIATE_KHR - immediate submission ?-> screen tearing
  KHRSurface/VK_PRESENT_MODE_FIFO_KHR - queue; vsync (waits to next vertical blank if the app is late)
  KHRSurface/VK_PRESENT_MODE_FIFO_RELAXED_KHR - visible screen tearing (doesn't wait to next vertical blank)
  KHRSurface/VK_PRESENT_MODE_MAILBOX_KHR - triple buffering - if there is time to render screens between
      vertical blanks, they don't have to wait and can simply be replaced with the newer ones -> better latency."
  [^IntBuffer present-modes-ptr present-modes-count]
  (or (some (fn [^long i]
              (when (= (.get present-modes-ptr i) KHRSurface/VK_PRESENT_MODE_MAILBOX_KHR)
                KHRSurface/VK_PRESENT_MODE_MAILBOX_KHR))
            (range present-modes-count))
      KHRSurface/VK_PRESENT_MODE_FIFO_KHR))