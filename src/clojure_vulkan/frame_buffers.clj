(ns clojure-vulkan.frame-buffers
  (:require [clojure-vulkan.util :as util])
  (:import (clojure_vulkan.Vulkan VulkanGlobals VulkanGlobalsInterfaces$VkPointer VulkanGlobalsInterfaces$VkPointerVector)
           (java.util Collection Vector)
           (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK13 VkFramebufferCreateInfo)))

(defn create-frame-buffers []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [attachments-buffer (.mallocLong stack 1)
          frame-buffer-ptr (.mallocLong stack 1)
          frame-buffer-create-info (doto (VkFramebufferCreateInfo/calloc stack)
                                     (.sType VK13/VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                                     (.renderPass (.get VulkanGlobals/RENDER_PASS_POINTER))
                                     (.attachmentCount 1)
                                     (.width (.width (.get VulkanGlobals/SWAP_CHAIN_EXTENT)))
                                     (.height (.height (.get VulkanGlobals/SWAP_CHAIN_EXTENT)))
                                     (.layers 1))]
      (.set VulkanGlobals/SWAP_CHAIN_FRAME_BUFFER_POINTERS
            (VulkanGlobalsInterfaces$VkPointerVector/asVkPointerVector
              (Vector. ^Collection
                       (mapv (fn [^VulkanGlobalsInterfaces$VkPointer image-view-ptr]
                               (.put attachments-buffer 0 (.get image-view-ptr))
                               (.pAttachments frame-buffer-create-info attachments-buffer)
                               (if (= (VK13/vkCreateFramebuffer (VulkanGlobals/getLogicalDevice) frame-buffer-create-info nil frame-buffer-ptr)
                                      VK13/VK_SUCCESS)
                                 (.get frame-buffer-ptr 0)
                                 (throw (RuntimeException. "Failed to create framebuffer."))))
                             (.get (VulkanGlobals/SWAP_CHAIN_IMAGE_VIEWS_POINTERS)))))))))
