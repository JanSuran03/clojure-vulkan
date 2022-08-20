(ns clojure-vulkan.buffer
  (:require [clojure-vulkan.globals :refer [LOGICAL-DEVICE]])
  (:import (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VkBufferCreateInfo VK13 VkMemoryRequirements VkMemoryAllocateInfo)))

(defn create-buffer [^Integer byte-size ^Integer usage ^Integer property-flags ^MemoryStack stack]
  (let [buffer-ptr* (.mallocLong stack 1)
        buffer-memory-ptr* (.mallocLong stack 1)
        buffer-create-info (doto (VkBufferCreateInfo/calloc stack)
                             (.sType VK13/VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                             (.size byte-size)
                             (.usage usage)
                             (.sharingMode VK13/VK_SHARING_MODE_EXCLUSIVE))
        ^long buffer-pointer (if (= (VK13/vkCreateBuffer LOGICAL-DEVICE buffer-create-info nil buffer-ptr*)
                                    VK13/VK_SUCCESS)
                               (.get buffer-ptr* 0)
                               (throw (RuntimeException. "Failed to create buffer.")))]
    (try
      (let [memory-requirements (VkMemoryRequirements/calloc stack)
            _ (VK13/vkGetBufferMemoryRequirements LOGICAL-DEVICE buffer-pointer memory-requirements)
            vk-memory-allocate-info (doto (VkMemoryAllocateInfo/calloc stack)
                                      (.sType VK13/VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                                      (.allocationSize (.size memory-requirements))
                                      (.memoryTypeIndex '(find-memory-type property-flags ...)))
            ^long buffer-memory-pointer (if (= (VK13/vkAllocateMemory LOGICAL-DEVICE vk-memory-allocate-info nil buffer-memory-ptr*)
                                               VK13/VK_SUCCESS)
                                          (.get buffer-memory-ptr* 0)
                                          (throw (RuntimeException. "Failed to allocate buffer memory.")))]
        (try (VK13/vkBindBufferMemory LOGICAL-DEVICE buffer-pointer buffer-memory-pointer 0)
             (catch Throwable t
               (println "Failed to bind buffer memory: deallocating memory.")
               (VK13/vkFreeMemory LOGICAL-DEVICE buffer-memory-pointer nil)
               (throw t)))
        [buffer-pointer buffer-memory-pointer])
      (catch Throwable t
        (println "Error in memory buffer allocation process: deleting assigned buffer.")
        (VK13/vkDestroyBuffer LOGICAL-DEVICE buffer-pointer nil)
        (throw t)))))