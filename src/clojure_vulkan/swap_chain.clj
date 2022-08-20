(ns clojure-vulkan.swap-chain
  (:require [clojure-vulkan.frame-buffers :as frame-buffers]
            [clojure-vulkan.globals :as globals :refer [LOGICAL-DEVICE PHYSICAL-DEVICE QUEUE-FAMILIES SWAP-CHAIN-EXTENT SWAP-CHAIN-IMAGE-FORMAT
                                                        SWAP-CHAIN-IMAGES SWAP-CHAIN-POINTER SWAP-CHAIN-SUPPORT-DETAILS WINDOW-POINTER WINDOW-SURFACE-POINTER]]
            [clojure-vulkan.image-views :as image-views]
            [clojure-vulkan.util :as util]
            [clojure-vulkan.command-buffers :as command-buffers]
            [clojure-vulkan.graphics-pipeline :as graphics-pipeline]
            [clojure-vulkan.render-pass :as render-pass])
  (:import (java.nio IntBuffer)
           (org.lwjgl.glfw GLFW)
           (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan KHRSurface KHRSwapchain VK13 VkExtent2D VkPhysicalDevice VkSurfaceCapabilitiesKHR VkSurfaceFormatKHR VkSurfaceFormatKHR$Buffer VkSwapchainCreateInfoKHR)))

(defonce UINT32-MAX 0xffffffff)

(defn query-swap-chain-support [^VkPhysicalDevice device]
  (util/with-memory-stack-push ^MemoryStack stack
    (let [^VkSurfaceCapabilitiesKHR surface-capabilities (VkSurfaceCapabilitiesKHR/malloc)
          _ (KHRSurface/vkGetPhysicalDeviceSurfaceCapabilitiesKHR device WINDOW-SURFACE-POINTER surface-capabilities)
          formats-count-ptr (.ints stack 0)
          _ (KHRSurface/vkGetPhysicalDeviceSurfaceFormatsKHR device WINDOW-SURFACE-POINTER formats-count-ptr nil)
          formats-count (.get formats-count-ptr 0)
          formats (when-not (zero? formats-count)
                    (let [formats-ptr (VkSurfaceFormatKHR/malloc formats-count)]
                      (KHRSurface/vkGetPhysicalDeviceSurfaceFormatsKHR device WINDOW-SURFACE-POINTER formats-count-ptr formats-ptr)
                      formats-ptr))
          present-mode-count-ptr (.ints stack 0)
          _ (KHRSurface/vkGetPhysicalDeviceSurfacePresentModesKHR device WINDOW-SURFACE-POINTER present-mode-count-ptr nil)
          present-modes-count (.get present-mode-count-ptr 0)
          present-modes-ptr (when-not (zero? present-modes-count)
                              (let [present-modes-ptr (.mallocInt stack present-modes-count)]
                                (KHRSurface/vkGetPhysicalDeviceSurfacePresentModesKHR device WINDOW-SURFACE-POINTER present-mode-count-ptr present-modes-ptr)
                                present-modes-ptr))]
      (and formats present-modes-ptr
           (globals/set-global! SWAP-CHAIN-SUPPORT-DETAILS {:formats-ptr          formats
                                                            :present-modes-ptr    present-modes-ptr
                                                            :present-modes-count  present-modes-count
                                                            :surface-capabilities surface-capabilities})))))

(defn ^VkSurfaceFormatKHR choose-swap-surface-format [^VkSurfaceFormatKHR$Buffer formats-ptr]
  (or (->> (util/struct-buffer->seq formats-ptr)
           (some (fn [^VkSurfaceFormatKHR format]
                   (and (= (.format format) VK13/VK_FORMAT_B8G8R8_UNORM)
                        (= (.colorSpace format) KHRSurface/VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                        format))))
      (.get formats-ptr 0)))

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

(defn ^VkExtent2D choose-swap-extent [^VkSurfaceCapabilitiesKHR surface-capabilities]
  (if (= (.. surface-capabilities currentExtent width) UINT32-MAX)
    (util/with-memory-stack-push ^MemoryStack stack
      (let [width-ptr (.mallocInt stack 1)
            height-ptr (.mallocInt stack 1)
            _ (GLFW/glfwGetFramebufferSize WINDOW-POINTER width-ptr height-ptr)
            min-extent (.minImageExtent surface-capabilities)
            max-extent (.maxImageExtent surface-capabilities)]
        (doto (VkExtent2D/malloc stack)
          (.set (util/clamp (.width min-extent) (.get width-ptr 0) (.width max-extent))
                (util/clamp (.height min-extent) (.get height-ptr 0) (.height max-extent))))))
    (.currentExtent surface-capabilities)))

(defn create-swap-chain []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [{:keys [formats-ptr present-modes-ptr present-modes-count ^VkSurfaceCapabilitiesKHR surface-capabilities]}
          (or                                               ;(not-empty SWAP-CHAIN-SUPPORT-DETAILS)
            (query-swap-chain-support PHYSICAL-DEVICE))
          surface-format (choose-swap-surface-format formats-ptr)
          present-mode (choose-swap-presentation-mode present-modes-ptr present-modes-count)
          extent (choose-swap-extent surface-capabilities)
          image-count (inc (.minImageCount surface-capabilities))
          image-count-ptr (if (and (pos? (.maxImageCount surface-capabilities))
                                   (> image-count (.maxImageCount surface-capabilities)))
                            (.ints stack (.maxImageCount surface-capabilities))
                            (.ints stack ^Integer (inc (.minImageCount surface-capabilities))))
          ^VkSwapchainCreateInfoKHR swap-chain-create-info (doto (VkSwapchainCreateInfoKHR/calloc stack)
                                                             (.sType KHRSwapchain/VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                                                             (.surface WINDOW-SURFACE-POINTER)
                                                             (.minImageCount image-count)
                                                             (.imageFormat (.format surface-format))
                                                             (.imageColorSpace (.colorSpace surface-format))
                                                             (.imageExtent extent)
                                                             (.imageArrayLayers 1)
                                                             (.imageUsage VK13/VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT))
          _ (do (if (= (:graphics-family QUEUE-FAMILIES) (:present-family QUEUE-FAMILIES))
                  (doto swap-chain-create-info
                    (.imageSharingMode VK13/VK_SHARING_MODE_EXCLUSIVE)
                    (.queueFamilyIndexCount 0))
                  (doto swap-chain-create-info
                    (.imageSharingMode VK13/VK_SHARING_MODE_CONCURRENT)
                    (.queueFamilyIndexCount 2)
                    (.pQueueFamilyIndices (.ints stack (:graphics-family QUEUE-FAMILIES) (:present-family QUEUE-FAMILIES)))))
                (doto swap-chain-create-info
                  (.preTransform (.currentTransform surface-capabilities))
                  (.compositeAlpha KHRSurface/VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                  (.presentMode present-mode)
                  (.clipped true)))
          swap-chain-ptr* (.longs stack VK13/VK_NULL_HANDLE)
          _ (do (when (not= (KHRSwapchain/vkCreateSwapchainKHR LOGICAL-DEVICE swap-chain-create-info nil swap-chain-ptr*)
                            VK13/VK_SUCCESS)
                  (throw (RuntimeException. "Failed to create swapchain.")))
                (globals/set-global! SWAP-CHAIN-POINTER (.get swap-chain-ptr* 0))
                (KHRSwapchain/vkGetSwapchainImagesKHR LOGICAL-DEVICE SWAP-CHAIN-POINTER image-count-ptr nil))
          swapchain-images-ptr (.mallocLong stack (.get image-count-ptr 0))]
      (KHRSwapchain/vkGetSwapchainImagesKHR LOGICAL-DEVICE SWAP-CHAIN-POINTER image-count-ptr swapchain-images-ptr)
      (globals/set-global! SWAP-CHAIN-IMAGES (mapv #(.get swapchain-images-ptr ^int %)
                                                   (range image-count)))
      (globals/set-global! SWAP-CHAIN-IMAGE-FORMAT (.format surface-format))
      (globals/set-global! SWAP-CHAIN-EXTENT (doto (VkExtent2D/create)
                                               (.set extent))))))

(defn cleanup-swap-chain []
  (command-buffers/destroy-command-buffers)
  (graphics-pipeline/destroy-graphics-pipeline)
  (graphics-pipeline/destroy-pipeline-layout)
  (render-pass/destroy-render-pass)
  (frame-buffers/destroy-frame-buffers)
  (image-views/destroy-image-views)
  (KHRSwapchain/vkDestroySwapchainKHR LOGICAL-DEVICE SWAP-CHAIN-POINTER nil))

(defn recreate-swap-chain []
  (let [stack (MemoryStack/stackGet)
        width-buffer (.ints stack 0)
        height-buffer (.ints stack 0)]
    (GLFW/glfwGetFramebufferSize WINDOW-POINTER width-buffer height-buffer)
    (while (or (zero? (.get width-buffer 0))
               (zero? (.get height-buffer 0)))
      (GLFW/glfwGetFramebufferSize WINDOW-POINTER width-buffer height-buffer)
      (GLFW/glfwWaitEvents))
    (VK13/vkDeviceWaitIdle LOGICAL-DEVICE)
    (cleanup-swap-chain)

    (create-swap-chain)
    (image-views/create-image-views)
    (render-pass/create-render-pass)
    (graphics-pipeline/create-graphics-pipeline)
    (frame-buffers/create-frame-buffers)
    (command-buffers/create-command-buffers)))