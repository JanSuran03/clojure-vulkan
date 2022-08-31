(ns clojure-vulkan.texture
  (:require [clojure-vulkan.buffer :as buffer]
            [clojure-vulkan.globals :as globals :refer [IMAGE-MEMORY-POINTER IMAGE-POINTER]]
            [clojure-vulkan.util :as util])
  (:import (clojure_vulkan.Vulkan VulkanGlobals Buffer)
           (java.nio IntBuffer ByteBuffer LongBuffer)
           (org.lwjgl.stb STBImage)
           (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK13 VkImageCreateInfo VkMemoryAllocateInfo VkMemoryRequirements VkImageMemoryBarrier VkBufferImageCopy VkExtent3D VkOffset3D)))

(def textures-root "resources/textures/")

(defn destroy-texture []
  (VK13/vkDestroyImage (VulkanGlobals/getLogicalDevice) IMAGE-POINTER nil)
  (globals/reset-image-ptr))

(defn free-texture-memory []
  (VK13/vkFreeMemory (VulkanGlobals/getLogicalDevice) IMAGE-MEMORY-POINTER nil)
  (globals/reset-image-memory-ptr))

(defn transition-image-layout [image-pointer image-format old-layout new-layout]
  (util/with-memory-stack-push ^MemoryStack stack
    (let [command-buffer (util/begin-single-time-commands)
          image-memory-barriers (doto (VkImageMemoryBarrier/calloc 1 stack)
                                  (.sType VK13/VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                                  (.oldLayout old-layout)
                                  (.newLayout new-layout)
                                  (.srcQueueFamilyIndex VK13/VK_QUEUE_FAMILY_IGNORED)
                                  (.dstQueueFamilyIndex VK13/VK_QUEUE_FAMILY_IGNORED)
                                  (.image image-pointer)
                                  (.. subresourceRange (aspectMask VK13/VK_IMAGE_ASPECT_COLOR_BIT))
                                  (.. subresourceRange (baseMipLevel 0))
                                  (.. subresourceRange (levelCount 1))
                                  (.. subresourceRange (baseArrayLayer 0))
                                  (.. subresourceRange (layerCount 1))
                                  (.srcAccessMask 0)
                                  (.dstAccessMask 0))
          [source-stage-mask destination-stage-mask] (cond (and (= old-layout VK13/VK_IMAGE_LAYOUT_UNDEFINED)
                                                                (= new-layout VK13/VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL))
                                                           (do (.srcAccessMask image-memory-barriers 0)
                                                               (.dstAccessMask image-memory-barriers VK13/VK_ACCESS_TRANSFER_WRITE_BIT)
                                                               [VK13/VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT
                                                                VK13/VK_PIPELINE_STAGE_TRANSFER_BIT])

                                                           (and (= old-layout VK13/VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                                                                (= new-layout VK13/VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL))
                                                           (do (.srcAccessMask image-memory-barriers VK13/VK_ACCESS_TRANSFER_WRITE_BIT)
                                                               (.dstAccessMask image-memory-barriers VK13/VK_ACCESS_SHADER_READ_BIT)
                                                               [VK13/VK_PIPELINE_STAGE_TRANSFER_BIT
                                                                VK13/VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT])

                                                           :else
                                                           (throw (RuntimeException. "Unsupported layout transition.")))]
      (VK13/vkCmdPipelineBarrier command-buffer source-stage-mask destination-stage-mask
                                 #_dependency-flags 0
                                 #_memory-barriers nil
                                 #_buffer-memory-barriers nil
                                 image-memory-barriers)
      (util/end-single-time-commands command-buffer))))

(defn copy-buffer-to-image [staging-buffer-ptr image-pointer image-width image-height]
  (util/with-memory-stack-push ^MemoryStack stack
    (let [command-buffer (util/begin-single-time-commands)
          buffer-image-copy-region (doto (VkBufferImageCopy/calloc 1 stack)
                                     (.bufferOffset 0)
                                     (.bufferRowLength 0)
                                     (.bufferImageHeight 0)
                                     (.. imageSubresource (aspectMask VK13/VK_IMAGE_ASPECT_COLOR_BIT))
                                     (.. imageSubresource (mipLevel 0))
                                     (.. imageSubresource (baseArrayLayer 0))
                                     (.. imageSubresource (layerCount 1))
                                     (.imageOffset (.set (VkOffset3D/calloc stack) 0 0 0))
                                     (.imageExtent (.set (VkExtent3D/calloc stack) image-width image-height 1)))]
      (VK13/vkCmdCopyBufferToImage command-buffer staging-buffer-ptr image-pointer VK13/VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL buffer-image-copy-region)
      (util/end-single-time-commands command-buffer))))

(defn create-texture-image [texture-filepath]
  (util/with-memory-stack-push ^MemoryStack stack
    (let [[^IntBuffer texture-width* ^IntBuffer texture-height* ^IntBuffer texture-channels] (repeatedly #(.mallocInt stack 1))
          filepath (str textures-root texture-filepath)
          ^ByteBuffer pixels (STBImage/stbi_load (.UTF8 stack filepath) texture-width* texture-height* texture-channels STBImage/STBI_rgb_alpha)
          image-size (* STBImage/STBI_rgb_alpha (.get texture-width* 0) (.get texture-height* 0))
          _ (when (nil? pixels)
              (throw (RuntimeException. (str "Texture " texture-filepath " couldn't be loaded."))))
          [^LongBuffer buffer-ptr* ^LongBuffer buffer-memory-ptr*] (repeatedly #(.mallocLong stack 1))
          staging-buffer
          (buffer/create-buffer image-size VK13/VK_BUFFER_USAGE_TRANSFER_SRC_BIT (util/bit-ors VK13/VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                                                                                               VK13/VK_MEMORY_PROPERTY_HOST_COHERENT_BIT)
                                buffer-ptr* buffer-memory-ptr* stack)
          data-ptr* (.mallocPointer stack 1)
          _ (do (buffer/staging-buffer-memcpy (.bufferMemoryPointer staging-buffer) image-size data-ptr* pixels :buffer-copy/byte-buffer)
                (STBImage/stbi_image_free pixels))
          image-ptr* (.mallocLong stack 1)
          image-create-info (doto (VkImageCreateInfo/calloc stack)
                              (.sType VK13/VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                              (.imageType VK13/VK_IMAGE_TYPE_2D)
                              (.. extent (width (.get texture-width* 0)))
                              (.. extent (height (.get texture-height* 0)))
                              (.. extent (depth 1))
                              (.mipLevels 1)
                              (.arrayLayers 1)
                              (.format VK13/VK_FORMAT_R8G8B8A8_SRGB)
                              (.tiling VK13/VK_IMAGE_TILING_OPTIMAL)
                              (.initialLayout VK13/VK_IMAGE_LAYOUT_UNDEFINED)
                              (.usage (util/bit-ors VK13/VK_IMAGE_USAGE_TRANSFER_DST_BIT
                                                    VK13/VK_IMAGE_USAGE_SAMPLED_BIT))
                              (.sharingMode VK13/VK_SHARING_MODE_EXCLUSIVE)
                              (.samples VK13/VK_SAMPLE_COUNT_1_BIT)
                              (.flags 0))
          _ (if (= (VK13/vkCreateImage (VulkanGlobals/getLogicalDevice) image-create-info nil image-ptr*)
                   VK13/VK_SUCCESS)
              (globals/set-global! IMAGE-POINTER (.get image-ptr* 0))
              (throw (RuntimeException. (str "Failed to create image: " texture-filepath))))
          memory-requirements (VkMemoryRequirements/calloc stack)
          _ (VK13/vkGetImageMemoryRequirements (VulkanGlobals/getLogicalDevice) IMAGE-POINTER memory-requirements)
          memory-allocate-info (doto (VkMemoryAllocateInfo/calloc stack)
                                 (.sType VK13/VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                                 (.allocationSize (.size memory-requirements))
                                 (.memoryTypeIndex (util/find-memory-type (.memoryTypeBits memory-requirements)
                                                                          VK13/VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT
                                                                          stack :memory-kind/image)))
          image-memory-ptr* (.mallocLong stack 1)
          _ (do (if (= (VK13/vkAllocateMemory (VulkanGlobals/getLogicalDevice) memory-allocate-info nil image-memory-ptr*)
                       VK13/VK_SUCCESS)
                  (globals/set-global! IMAGE-MEMORY-POINTER (.get image-memory-ptr* 0))
                  (throw (RuntimeException. "Failed to allocate image memory.")))
                (VK13/vkBindImageMemory (VulkanGlobals/getLogicalDevice) IMAGE-POINTER IMAGE-MEMORY-POINTER 0))]
      (transition-image-layout IMAGE-POINTER VK13/VK_FORMAT_R8G8B8A8_SRGB VK13/VK_IMAGE_LAYOUT_UNDEFINED VK13/VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
      (copy-buffer-to-image (.bufferPointer staging-buffer) IMAGE-POINTER (.get texture-width* 0) (.get texture-height* 0))
      (transition-image-layout IMAGE-POINTER VK13/VK_FORMAT_R8G8B8A8_SRGB VK13/VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL VK13/VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
      (.free ^Buffer staging-buffer))))