(ns clojure-vulkan.buffer
  (:require [clojure-vulkan.globals :refer [COMMAND-POOL-POINTER GRAPHICS-QUEUE LOGICAL-DEVICE PHYSICAL-DEVICE]])
  (:import (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK13 VkBufferCreateInfo VkMemoryAllocateInfo VkMemoryRequirements
                             VkPhysicalDeviceMemoryProperties VkCommandBufferAllocateInfo VkCommandBuffer VkCommandBufferBeginInfo VkBufferCopy VkSubmitInfo)
           (java.nio LongBuffer)))

(defn- find-memory-type [^Integer type-filter ^Integer memory-property-flags ^MemoryStack stack]
  (let [memory-properties (VkPhysicalDeviceMemoryProperties/malloc stack)]
    (VK13/vkGetPhysicalDeviceMemoryProperties PHYSICAL-DEVICE memory-properties)
    (or (some (fn [^Integer i]
                (when (and (not= 0 (bit-and type-filter (bit-shift-left 1 i)))
                           (= (bit-and (.propertyFlags (.memoryTypes memory-properties i))
                                       memory-property-flags)
                              memory-property-flags))
                  i))
              (range (.memoryTypeCount memory-properties)))
        (throw (RuntimeException. "Failed to find suitable memory type for the vertex buffer.")))))

(defn create-buffer [^Integer byte-size ^Integer usage ^Integer property-flags
                     ^LongBuffer buffer-ptr* ^LongBuffer buffer-memory-ptr* ^MemoryStack stack]
  (let [buffer-create-info (doto (VkBufferCreateInfo/calloc stack)
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
                                      (.memoryTypeIndex (find-memory-type (.memoryTypeBits memory-requirements)
                                                                          property-flags
                                                                          stack)))
            ^long buffer-memory-pointer (if (= (VK13/vkAllocateMemory LOGICAL-DEVICE vk-memory-allocate-info nil buffer-memory-ptr*)
                                               VK13/VK_SUCCESS)
                                          (.get buffer-memory-ptr* 0)
                                          (throw (RuntimeException. "Failed to allocate buffer memory.")))]
        (try (VK13/vkBindBufferMemory LOGICAL-DEVICE buffer-pointer buffer-memory-pointer 0)
             (catch Throwable t
               (println "Failed to bind buffer memory: deallocating memory.")
               (VK13/vkFreeMemory LOGICAL-DEVICE buffer-memory-pointer nil)
               (throw t)))
        [buffer-pointer buffer-memory-pointer buffer-create-info])
      (catch Throwable t
        (println "Error in memory buffer allocation process: deleting assigned buffer.")
        (VK13/vkDestroyBuffer LOGICAL-DEVICE buffer-pointer nil)
        (throw t)))))

(defn copy-buffer [src-buffer-ptr dest-buffer-ptr buffer-size ^MemoryStack stack]
  (let [command-buffer-allocate-info (doto (VkCommandBufferAllocateInfo/calloc stack)
                                       (.sType VK13/VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                                       (.level VK13/VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                                       (.commandPool COMMAND-POOL-POINTER)
                                       (.commandBufferCount 1))
        command-buffer-ptr (.mallocPointer stack 1)
        _ (VK13/vkAllocateCommandBuffers LOGICAL-DEVICE command-buffer-allocate-info command-buffer-ptr)
        command-buffer (VkCommandBuffer. (.get command-buffer-ptr 0) LOGICAL-DEVICE)
        command-buffer-begin-info (doto (VkCommandBufferBeginInfo/calloc stack)
                                    (.sType VK13/VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                                    (.flags VK13/VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT))
        _ (VK13/vkBeginCommandBuffer command-buffer command-buffer-begin-info)
        buffer-copy-region (doto (VkBufferCopy/calloc 1 stack)
                             (.srcOffset 0)
                             (.dstOffset 0)
                             (.size buffer-size))
        _ (VK13/vkCmdCopyBuffer command-buffer src-buffer-ptr dest-buffer-ptr buffer-copy-region)
        _ (VK13/vkEndCommandBuffer command-buffer)
        submit-info (doto (VkSubmitInfo/calloc stack)
                      (.sType VK13/VK_STRUCTURE_TYPE_SUBMIT_INFO)
                      (.pCommandBuffers command-buffer-ptr))]
    (when (not= (VK13/vkQueueSubmit GRAPHICS-QUEUE submit-info VK13/VK_NULL_HANDLE) ; fence
                VK13/VK_SUCCESS)
      (throw (RuntimeException. "Failed to submit copy command buffer.")))
    (VK13/vkDeviceWaitIdle LOGICAL-DEVICE)
    (VK13/vkFreeCommandBuffers LOGICAL-DEVICE COMMAND-POOL-POINTER command-buffer-ptr)))