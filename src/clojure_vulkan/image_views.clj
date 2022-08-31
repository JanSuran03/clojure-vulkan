(ns clojure-vulkan.image-views
  (:require [clojure-vulkan.util :as util])
  (:import (clojure_vulkan.Vulkan VulkanGlobals VulkanGlobalsIntefaces$VkPointer VulkanGlobalsIntefaces$VkPointerVector)
           (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK13 VkImageViewCreateInfo)
           (java.util Vector Collection)))

(defn create-image-views []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [image-view-ptr (.mallocLong stack 1)
          image-view-create-info (doto (VkImageViewCreateInfo/calloc stack)
                                   (.sType VK13/VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                                   (.viewType VK13/VK_IMAGE_VIEW_TYPE_2D)
                                   (.format (.get VulkanGlobals/SWAP_CHAIN_IMAGE_FORMAT)))
          _ (do (doto (.components image-view-create-info)
                  (.r VK13/VK_COMPONENT_SWIZZLE_IDENTITY)
                  (.g VK13/VK_COMPONENT_SWIZZLE_IDENTITY)
                  (.b VK13/VK_COMPONENT_SWIZZLE_IDENTITY)
                  (.a VK13/VK_COMPONENT_SWIZZLE_IDENTITY))
                (doto (.subresourceRange image-view-create-info)
                  (.aspectMask VK13/VK_IMAGE_ASPECT_COLOR_BIT)
                  (.baseMipLevel 0)
                  (.levelCount 1)
                  (.baseArrayLayer 0)
                  (.layerCount 1)))
          image-views (mapv (fn [^VulkanGlobalsIntefaces$VkPointer swap-chain-image]
                              (.image image-view-create-info (.get swap-chain-image))
                              (if (= (VK13/vkCreateImageView (VulkanGlobals/getLogicalDevice) image-view-create-info nil image-view-ptr)
                                     VK13/VK_SUCCESS)
                                (.get image-view-ptr 0)
                                (throw (RuntimeException. "Couldn't create image views."))))
                            (.get VulkanGlobals/SWAP_CHAIN_IMAGE_POINTERS))]
      (.set VulkanGlobals/SWAP_CHAIN_IMAGE_VIEWS_POINTERS
            (VulkanGlobalsIntefaces$VkPointerVector/asVkPointerVector
              (Vector. ^Collection image-views))))))
