(ns clojure-vulkan.texture
  (:require [clojure-vulkan.buffer :as buffer]
            [clojure-vulkan.globals :as globals :refer [IMAGE-MEMORY-POINTER IMAGE-POINTER]]
            [clojure-vulkan.util :as util])
  (:import (clojure_vulkan.Vulkan VulkanGlobals Buffer)
           (java.nio IntBuffer ByteBuffer LongBuffer)
           (org.lwjgl.stb STBImage)
           (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK13 VkImageCreateInfo VkMemoryAllocateInfo VkMemoryRequirements)))

(def textures-root "resources/textures/")

(defn create-texture-image [texture-filepath]
  (util/with-memory-stack-push ^MemoryStack stack
    (let [[^IntBuffer texture-width ^IntBuffer texture-height ^IntBuffer texture-channels] (repeatedly #(.mallocInt stack 1))
          filepath (str textures-root texture-filepath)
          ^ByteBuffer pixels (STBImage/stbi_load (.UTF8 stack filepath) texture-width texture-height texture-channels STBImage/STBI_rgb_alpha)
          image-size (* STBImage/STBI_rgb_alpha (.get texture-width 0) (.get texture-height 0))
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
          _ (.free ^Buffer staging-buffer)
          image-ptr* (.mallocLong stack 1)
          image-create-info (doto (VkImageCreateInfo/calloc stack)
                              (.sType VK13/VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                              (.imageType VK13/VK_IMAGE_TYPE_2D)
                              (.. extent (width (.get texture-width 0)))
                              (.. extent (height (.get texture-height 0)))
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
          image-memory-ptr* (.mallocLong stack 1)]
      (if (= (VK13/vkAllocateMemory (VulkanGlobals/getLogicalDevice) memory-allocate-info nil image-memory-ptr*)
             VK13/VK_SUCCESS)
        (globals/set-global! IMAGE-MEMORY-POINTER (.get image-memory-ptr* 0))
        (throw (RuntimeException. "Failed to allocate image memory.")))
      (VK13/vkBindImageMemory (VulkanGlobals/getLogicalDevice) IMAGE-POINTER IMAGE-MEMORY-POINTER 0))))

(defn destroy-texture []
  (VK13/vkDestroyImage (VulkanGlobals/getLogicalDevice) IMAGE-POINTER nil)
  (globals/reset-image-ptr))

(defn free-texture-memory []
  (VK13/vkFreeMemory (VulkanGlobals/getLogicalDevice) IMAGE-MEMORY-POINTER nil)
  (globals/reset-image-memory-ptr))