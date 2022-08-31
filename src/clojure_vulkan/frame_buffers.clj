(ns clojure-vulkan.frame-buffers
  (:require [clojure-vulkan.globals :as globals :refer [RENDER-PASS-POINTER SWAP-CHAIN-IMAGE-VIEWS-POINTERS
                                                        SWAP-CHAIN-FRAME-BUFFER-POINTERS]]
            [clojure-vulkan.util :as util])
  (:import (clojure_vulkan.Vulkan VulkanGlobals)
           (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK13 VkFramebufferCreateInfo)))

(defn create-frame-buffers []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [attachments-buffer (.mallocLong stack 1)
          frame-buffer-ptr (.mallocLong stack 1)
          frame-buffer-create-info (doto (VkFramebufferCreateInfo/calloc stack)
                                     (.sType VK13/VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                                     (.renderPass RENDER-PASS-POINTER)
                                     (.attachmentCount 1)
                                     (.width (.width (.get VulkanGlobals/SWAP_CHAIN_EXTENT)))
                                     (.height (.height (.get VulkanGlobals/SWAP_CHAIN_EXTENT)))
                                     (.layers 1))]
      (doseq [^Long image-view SWAP-CHAIN-IMAGE-VIEWS-POINTERS]
        (.put attachments-buffer 0 image-view)
        (.pAttachments frame-buffer-create-info attachments-buffer)
        (if (= (VK13/vkCreateFramebuffer (VulkanGlobals/getLogicalDevice) frame-buffer-create-info nil frame-buffer-ptr)
               VK13/VK_SUCCESS)
          (alter-var-root #'SWAP-CHAIN-FRAME-BUFFER-POINTERS conj (.get frame-buffer-ptr 0))
          (throw (RuntimeException. "Failed to create framebuffer.")))))))

(defn destroy-frame-buffers []
  (doseq [swap-chain-frame-buffer-ptr SWAP-CHAIN-FRAME-BUFFER-POINTERS]
    (VK13/vkDestroyFramebuffer (VulkanGlobals/getLogicalDevice) ^long swap-chain-frame-buffer-ptr nil)
    (globals/reset-swap-chain-frame-buffers)))