(ns clojure-vulkan.buffer
  (:require [clojure-vulkan.util :as util])
  (:import (clojure_vulkan MemoryUtils UniformBufferObject)
           (clojure_vulkan.Vulkan VulkanGlobals Buffer)
           (java.nio LongBuffer ByteBuffer)
           (org.lwjgl PointerBuffer)
           (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK13 VkBufferCopy VkBufferCreateInfo VkCommandBuffer VkCommandBufferAllocateInfo
                             VkCommandBufferBeginInfo VkMemoryAllocateInfo VkMemoryRequirements
                             VkSubmitInfo)))

(defn ^Buffer create-buffer [^Integer byte-size ^Integer usage ^Integer property-flags
                             ^LongBuffer buffer-ptr* ^LongBuffer buffer-memory-ptr* ^MemoryStack stack]
  (let [buffer-create-info (doto (VkBufferCreateInfo/calloc stack)
                             (.sType VK13/VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                             (.size byte-size)
                             (.usage usage)
                             (.sharingMode VK13/VK_SHARING_MODE_EXCLUSIVE))
        ^long buffer-pointer (if (= (VK13/vkCreateBuffer (VulkanGlobals/getLogicalDevice) buffer-create-info nil buffer-ptr*)
                                    VK13/VK_SUCCESS)
                               (.get buffer-ptr* 0)
                               (throw (RuntimeException. "Failed to create buffer.")))]
    (try
      (let [memory-requirements (VkMemoryRequirements/calloc stack)
            _ (VK13/vkGetBufferMemoryRequirements (VulkanGlobals/getLogicalDevice) buffer-pointer memory-requirements)
            vk-memory-allocate-info (doto (VkMemoryAllocateInfo/calloc stack)
                                      (.sType VK13/VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                                      (.allocationSize (.size memory-requirements))
                                      (.memoryTypeIndex (util/find-memory-type (.memoryTypeBits memory-requirements)
                                                                               property-flags
                                                                               stack
                                                                               :memory-kind/vertex-buffer)))
            ^long buffer-memory-pointer (if (= (VK13/vkAllocateMemory (VulkanGlobals/getLogicalDevice) vk-memory-allocate-info nil buffer-memory-ptr*)
                                               VK13/VK_SUCCESS)
                                          (.get buffer-memory-ptr* 0)
                                          (throw (RuntimeException. "Failed to allocate buffer memory.")))]
        (try (VK13/vkBindBufferMemory (VulkanGlobals/getLogicalDevice) buffer-pointer buffer-memory-pointer 0)
             (catch Throwable t
               (util/log "Failed to bind buffer memory: deallocating memory.")
               (VK13/vkFreeMemory (VulkanGlobals/getLogicalDevice) buffer-memory-pointer nil)
               (throw t)))
        (doto (Buffer.)
          (.bufferPointer buffer-pointer)
          (.bufferMemoryPointer buffer-memory-pointer)
          (.bufferCreateInfo buffer-create-info)))
      (catch Throwable t
        (util/log "Error in memory buffer allocation process: deleting assigned buffer.")
        (VK13/vkDestroyBuffer (VulkanGlobals/getLogicalDevice) buffer-pointer nil)
        (throw t)))))

(defn copy-buffer [src-buffer-ptr dest-buffer-ptr buffer-size ^MemoryStack stack]
  (let [command-buffer-allocate-info (doto (VkCommandBufferAllocateInfo/calloc stack)
                                       (.sType VK13/VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                                       (.level VK13/VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                                       (.commandPool (.get VulkanGlobals/COMMAND_POOL))
                                       (.commandBufferCount 1))
        command-buffer-ptr (.mallocPointer stack 1)
        _ (VK13/vkAllocateCommandBuffers (VulkanGlobals/getLogicalDevice) command-buffer-allocate-info command-buffer-ptr)
        command-buffer (VkCommandBuffer. (.get command-buffer-ptr 0) (VulkanGlobals/getLogicalDevice))
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
    (when (not= (VK13/vkQueueSubmit (.get VulkanGlobals/GRAPHICS_QUEUE) submit-info VK13/VK_NULL_HANDLE) ; fence
                VK13/VK_SUCCESS)
      (throw (RuntimeException. "Failed to submit copy command buffer.")))
    (VK13/vkDeviceWaitIdle (VulkanGlobals/getLogicalDevice))
    (VK13/vkFreeCommandBuffers (VulkanGlobals/getLogicalDevice) (.get VulkanGlobals/COMMAND_POOL) command-buffer-ptr)))

(defmulti ^:private do-buffer-memcpy (fn [mode & _]
                                       mode))

(defmethod do-buffer-memcpy :buffer-copy/floats
  [_mode data-ptr* ^"[F" data byte-size]
  (MemoryUtils/memcpyFloats (.getByteBuffer ^PointerBuffer data-ptr* 0 byte-size) data))

(defmethod do-buffer-memcpy :buffer-copy/byte-buffer
  [_mode data-ptr* ^ByteBuffer data byte-size]
  (MemoryUtils/memCpyByteBuffer (.getByteBuffer ^PointerBuffer data-ptr* 0 byte-size) data byte-size))

(defmethod do-buffer-memcpy :buffer-copy/shorts
  [_mode data-ptr* ^"[S" data byte-size]
  (MemoryUtils/memcpyShorts (.getByteBuffer ^PointerBuffer data-ptr* 0 byte-size) data))

(defmethod do-buffer-memcpy :buffer-copy/integers
  [_mode data-ptr* ^"[I" data byte-size]
  (MemoryUtils/memcpyIntegers (.getByteBuffer ^PointerBuffer data-ptr* 0 byte-size) data))

(defmethod do-buffer-memcpy :buffer-copy/uniform-buffer-object
  [_mode data-ptr* ^UniformBufferObject data byte-size]
  (MemoryUtils/memcpyUBO (.getByteBuffer ^PointerBuffer data-ptr* 0 byte-size) data))

(defn staging-buffer-memcpy [staging-buffer-memory-ptr byte-size data-ptr* the-data mode]
  (VK13/vkMapMemory (VulkanGlobals/getLogicalDevice) staging-buffer-memory-ptr 0 byte-size 0 data-ptr*)
  (do-buffer-memcpy mode data-ptr* the-data byte-size)
  (VK13/vkUnmapMemory (VulkanGlobals/getLogicalDevice) staging-buffer-memory-ptr))