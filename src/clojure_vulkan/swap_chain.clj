(ns clojure-vulkan.swap-chain
  (:require [clojure-vulkan.command-buffers :as command-buffers]
            [clojure-vulkan.frame-buffers :as frame-buffers]
            [clojure-vulkan.graphics-pipeline :as graphics-pipeline]
            [clojure-vulkan.image-views :as image-views]
            [clojure-vulkan.render-pass :as render-pass]
            [clojure-vulkan.util :as util])
  (:import (clojure_vulkan.Vulkan VulkanGlobals VulkanGlobalsInterfaces$VkPointerVector)
           (java.nio IntBuffer)
           (org.lwjgl.glfw GLFW)
           (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan KHRSurface KHRSwapchain VK13 VkExtent2D VkPhysicalDevice VkSurfaceCapabilitiesKHR VkSurfaceFormatKHR
                             VkSurfaceFormatKHR$Buffer VkSwapchainCreateInfoKHR)
           (java.util Vector Collection)))

(defonce UINT32-MAX 0xffffffff)

(defn query-swap-chain-support [^VkPhysicalDevice device]
  (util/with-memory-stack-push ^MemoryStack stack
    (let [^VkSurfaceCapabilitiesKHR surface-capabilities (VkSurfaceCapabilitiesKHR/malloc)
          _ (KHRSurface/vkGetPhysicalDeviceSurfaceCapabilitiesKHR device (.get VulkanGlobals/WINDOW_SURFACE_POINTER) surface-capabilities)
          formats-count-ptr (.ints stack 0)
          _ (KHRSurface/vkGetPhysicalDeviceSurfaceFormatsKHR device (.get VulkanGlobals/WINDOW_SURFACE_POINTER) formats-count-ptr nil)
          formats-count (.get formats-count-ptr 0)
          formats (when-not (zero? formats-count)
                    (let [formats-ptr (VkSurfaceFormatKHR/malloc formats-count)]
                      (KHRSurface/vkGetPhysicalDeviceSurfaceFormatsKHR device (.get VulkanGlobals/WINDOW_SURFACE_POINTER) formats-count-ptr formats-ptr)
                      formats-ptr))
          present-mode-count-ptr (.ints stack 0)
          _ (KHRSurface/vkGetPhysicalDeviceSurfacePresentModesKHR device (.get VulkanGlobals/WINDOW_SURFACE_POINTER) present-mode-count-ptr nil)
          present-modes-count (.get present-mode-count-ptr 0)
          present-modes-ptr (when-not (zero? present-modes-count)
                              (let [present-modes-ptr (.mallocInt stack present-modes-count)]
                                (KHRSurface/vkGetPhysicalDeviceSurfacePresentModesKHR device (.get VulkanGlobals/WINDOW_SURFACE_POINTER) present-mode-count-ptr present-modes-ptr)
                                present-modes-ptr))]
      (and formats present-modes-ptr
           {:formats-ptr          formats
            :present-modes-ptr    present-modes-ptr
            :surface-capabilities surface-capabilities}))))

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
  [^IntBuffer present-modes-ptr]
  (or (some (fn [^long i]
              (when (= (.get present-modes-ptr i) KHRSurface/VK_PRESENT_MODE_MAILBOX_KHR)
                KHRSurface/VK_PRESENT_MODE_MAILBOX_KHR))
            (range (.capacity present-modes-ptr)))
      KHRSurface/VK_PRESENT_MODE_FIFO_KHR))

(defn ^VkExtent2D choose-swap-extent [^VkSurfaceCapabilitiesKHR surface-capabilities]
  (if (= (.. surface-capabilities currentExtent width) UINT32-MAX)
    (util/with-memory-stack-push ^MemoryStack stack
      (let [width-ptr (.mallocInt stack 1)
            height-ptr (.mallocInt stack 1)
            _ (GLFW/glfwGetFramebufferSize (.get VulkanGlobals/WINDOW_POINTER) width-ptr height-ptr)
            min-extent (.minImageExtent surface-capabilities)
            max-extent (.maxImageExtent surface-capabilities)]
        (doto (VkExtent2D/malloc stack)
          (.set (util/clamp (.width min-extent) (.get width-ptr 0) (.width max-extent))
                (util/clamp (.height min-extent) (.get height-ptr 0) (.height max-extent))))))
    (.currentExtent surface-capabilities)))

(defn create-swap-chain []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [{:keys [formats-ptr present-modes-ptr ^VkSurfaceCapabilitiesKHR surface-capabilities]}
          (query-swap-chain-support (.get VulkanGlobals/PHYSICAL_DEVICE))
          surface-format (choose-swap-surface-format formats-ptr)
          present-mode (choose-swap-presentation-mode present-modes-ptr)
          extent (choose-swap-extent surface-capabilities)
          image-count (inc (.minImageCount surface-capabilities))
          image-count-ptr (if (and (pos? (.maxImageCount surface-capabilities))
                                   (> image-count (.maxImageCount surface-capabilities)))
                            (.ints stack (.maxImageCount surface-capabilities))
                            (.ints stack ^Integer (inc (.minImageCount surface-capabilities))))
          ^VkSwapchainCreateInfoKHR swap-chain-create-info (doto (VkSwapchainCreateInfoKHR/calloc stack)
                                                             (.sType KHRSwapchain/VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                                                             (.surface (.get VulkanGlobals/WINDOW_SURFACE_POINTER))
                                                             (.minImageCount image-count)
                                                             (.imageFormat (.format surface-format))
                                                             (.imageColorSpace (.colorSpace surface-format))
                                                             (.imageExtent extent)
                                                             (.imageArrayLayers 1)
                                                             (.imageUsage VK13/VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT))
          _ (do (if (= (.graphicsFamily VulkanGlobals/QUEUE_FAMILIES) (.presentFamily VulkanGlobals/QUEUE_FAMILIES))
                  (doto swap-chain-create-info
                    (.imageSharingMode VK13/VK_SHARING_MODE_EXCLUSIVE)
                    (.queueFamilyIndexCount 0))
                  (doto swap-chain-create-info
                    (.imageSharingMode VK13/VK_SHARING_MODE_CONCURRENT)
                    (.queueFamilyIndexCount 2)
                    (.pQueueFamilyIndices (.ints stack (.graphicsFamily VulkanGlobals/QUEUE_FAMILIES) (.presentFamily VulkanGlobals/QUEUE_FAMILIES)))))
                (doto swap-chain-create-info
                  (.preTransform (.currentTransform surface-capabilities))
                  (.compositeAlpha KHRSurface/VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                  (.presentMode present-mode)
                  (.clipped true)))
          swap-chain-ptr* (.longs stack VK13/VK_NULL_HANDLE)
          _ (do (when (not= (KHRSwapchain/vkCreateSwapchainKHR (VulkanGlobals/getLogicalDevice) swap-chain-create-info nil swap-chain-ptr*)
                            VK13/VK_SUCCESS)
                  (throw (RuntimeException. "Failed to create swapchain.")))
                (.set VulkanGlobals/SWAP_CHAIN_POINTER (.get swap-chain-ptr* 0))
                (KHRSwapchain/vkGetSwapchainImagesKHR (VulkanGlobals/getLogicalDevice) (.get VulkanGlobals/SWAP_CHAIN_POINTER) image-count-ptr nil))
          swapchain-images-ptr (.mallocLong stack (.get image-count-ptr 0))]
      (KHRSwapchain/vkGetSwapchainImagesKHR (VulkanGlobals/getLogicalDevice) (.get VulkanGlobals/SWAP_CHAIN_POINTER) image-count-ptr swapchain-images-ptr)
      (.set VulkanGlobals/SWAP_CHAIN_IMAGE_POINTERS
            (VulkanGlobalsInterfaces$VkPointerVector/asVkPointerVector
              (Vector. ^Collection (mapv #(.get swapchain-images-ptr ^int %)
                                         (range image-count)))))
      (.set VulkanGlobals/SWAP_CHAIN_IMAGE_FORMAT (.format surface-format))
      (.set VulkanGlobals/SWAP_CHAIN_EXTENT (doto (VkExtent2D/create)
                                              (.set extent))))))

(defn cleanup-swap-chain []
  (.free VulkanGlobals/COMMAND_BUFFERS)
  (graphics-pipeline/destroy-graphics-pipeline)
  (.free VulkanGlobals/SWAP_CHAIN_IMAGE_FORMAT)
  (.free VulkanGlobals/PIPELINE_LAYOUT_POINTER)
  (.free VulkanGlobals/RENDER_PASS_POINTER)
  (.free VulkanGlobals/SWAP_CHAIN_FRAME_BUFFER_POINTERS)
  (.free VulkanGlobals/SWAP_CHAIN_IMAGE_VIEWS_POINTERS)
  (.free VulkanGlobals/SWAP_CHAIN_POINTER)
  (.free VulkanGlobals/SWAP_CHAIN_IMAGE_POINTERS))

(defn recreate-swap-chain []
  (let [stack (MemoryStack/stackGet)
        width-buffer (.ints stack 0)
        height-buffer (.ints stack 0)]
    (GLFW/glfwGetFramebufferSize (.get VulkanGlobals/WINDOW_POINTER) width-buffer height-buffer)
    (while (or (zero? (.get width-buffer 0))
               (zero? (.get height-buffer 0)))
      (GLFW/glfwGetFramebufferSize (.get VulkanGlobals/WINDOW_POINTER) width-buffer height-buffer)
      (GLFW/glfwWaitEvents))
    (VK13/vkDeviceWaitIdle (VulkanGlobals/getLogicalDevice))
    (cleanup-swap-chain)

    (create-swap-chain)
    (image-views/create-image-views)
    (render-pass/create-render-pass)
    (graphics-pipeline/create-graphics-pipeline)
    (frame-buffers/create-frame-buffers)
    (command-buffers/create-command-buffers)))