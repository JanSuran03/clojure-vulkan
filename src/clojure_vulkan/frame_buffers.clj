(ns clojure-vulkan.frame-buffers
  (:require [clojure-vulkan.globals :as globals :refer [LOGICAL-DEVICE RENDER-PASS-POINTER SWAP-CHAIN-EXTENT
                                                        SWAP-CHAIN-IMAGE-VIEWS-POINTERS SWAP-CHAIN-FRAME-BUFFER-POINTERS]]
            [clojure-vulkan.util :as util])
  (:import (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK13 VkExtent2D VkFramebufferCreateInfo)))

(defn create-frame-buffers []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [attachments-buffer (.mallocLong stack 1)
          frame-buffer-ptr (.mallocLong stack 1)
          frame-buffer-create-info (doto (VkFramebufferCreateInfo/calloc stack)
                                     (.sType VK13/VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                                     (.renderPass RENDER-PASS-POINTER)
                                     (.attachmentCount 1)
                                     (.width (.width ^VkExtent2D SWAP-CHAIN-EXTENT))
                                     (.height (.height ^VkExtent2D SWAP-CHAIN-EXTENT))
                                     (.layers 1))]
      (doseq [^Long image-view SWAP-CHAIN-IMAGE-VIEWS-POINTERS]
        (.put attachments-buffer 0 image-view)
        (.pAttachments frame-buffer-create-info attachments-buffer)
        (if (= (VK13/vkCreateFramebuffer LOGICAL-DEVICE frame-buffer-create-info nil frame-buffer-ptr)
               VK13/VK_SUCCESS)
          (alter-var-root #'SWAP-CHAIN-FRAME-BUFFER-POINTERS conj (.get frame-buffer-ptr 0))
          (throw (RuntimeException. "Failed to create framebuffer.")))))))

(defn destroy-frame-buffers []
  (doseq [^Long frame-buffer SWAP-CHAIN-FRAME-BUFFER-POINTERS]
    (VK13/vkDestroyFramebuffer LOGICAL-DEVICE frame-buffer nil)
    (globals/reset-swap-chain-frame-buffers)))