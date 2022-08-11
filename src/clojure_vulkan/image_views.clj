(ns clojure-vulkan.image-views
  (:require [clojure-vulkan.globals  :as globals :refer [LOGICAL-DEVICE SWAP-CHAIN-IMAGE-FORMAT SWAP-CHAIN-IMAGE-VIEWS SWAP-CHAIN-IMAGES]]
            [clojure-vulkan.util :as util])
  (:import (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK13 VkImageViewCreateInfo)))

(defn create-image-views []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [image-view-ptr (.mallocLong stack 1)
          image-views (mapv (fn [swap-chain-image]
                              (let [^VkImageViewCreateInfo create-info (doto (VkImageViewCreateInfo/calloc stack)
                                                                         (.sType VK13/VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                                                                         (.image swap-chain-image)
                                                                         (.viewType VK13/VK_IMAGE_VIEW_TYPE_2D)
                                                                         (.format SWAP-CHAIN-IMAGE-FORMAT))
                                    _ (do (doto (.components create-info)
                                            (.r VK13/VK_COMPONENT_SWIZZLE_IDENTITY)
                                            (.g VK13/VK_COMPONENT_SWIZZLE_IDENTITY)
                                            (.b VK13/VK_COMPONENT_SWIZZLE_IDENTITY)
                                            (.a VK13/VK_COMPONENT_SWIZZLE_IDENTITY))
                                          (doto (.subresourceRange create-info)
                                            (.aspectMask VK13/VK_IMAGE_ASPECT_COLOR_BIT)
                                            (.baseMipLevel 0)
                                            (.levelCount 1)
                                            (.baseArrayLayer 0)
                                            (.layerCount 1)))]
                                (if (= (VK13/vkCreateImageView LOGICAL-DEVICE create-info nil image-view-ptr)
                                       VK13/VK_SUCCESS)
                                  (.get image-view-ptr 0)
                                  (throw (RuntimeException. "Couldn't create image views.")))))
                            SWAP-CHAIN-IMAGES)]
      (alter-var-root #'SWAP-CHAIN-IMAGE-VIEWS (constantly image-views)))))

(defn destroy-image-views []
  (doseq [image-view-ptr SWAP-CHAIN-IMAGE-VIEWS]
    (VK13/vkDestroyImageView LOGICAL-DEVICE image-view-ptr nil))
  (globals/reset-swap-chain-image-views))