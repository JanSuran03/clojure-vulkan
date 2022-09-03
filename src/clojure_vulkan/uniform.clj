(ns clojure-vulkan.uniform
  (:require [clojure-vulkan.buffer :as buffer]
            [clojure-vulkan.globals :refer [TEXTURE]]
            [clojure-vulkan.util :as util]
            [clojure-vulkan.vertex :as vertex])
  (:import (clojure_vulkan UniformBufferObject)
           (clojure_vulkan.Vulkan Buffer VulkanGlobals VulkanGlobalsInterfaces$VkPointerVector)
           (java.util Collection Vector)
           (org.joml Matrix4f)
           (org.lwjgl.glfw GLFW)
           (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK13 VkDescriptorBufferInfo VkDescriptorPoolCreateInfo VkDescriptorPoolSize VkDescriptorSetAllocateInfo VkDescriptorSetLayoutBinding VkDescriptorSetLayoutCreateInfo VkWriteDescriptorSet VkDescriptorImageInfo)))

(defn create-descriptor-set-layout []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [descriptor-set-layout-bindings (VkDescriptorSetLayoutBinding/calloc 2 stack)
          _ (do (doto ^VkDescriptorSetLayoutBinding (.get descriptor-set-layout-bindings 0)
                  (.binding ^int (get-in vertex/current-triangle-vbo-characterictics [:uniform 0 :binding]))
                  (.descriptorType VK13/VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                  (.descriptorCount 1)                      ; the shader variable can represent an array of UBOs
                  (.stageFlags VK13/VK_SHADER_STAGE_VERTEX_BIT)
                  (.pImmutableSamplers nil))
                (doto ^VkDescriptorSetLayoutBinding (.get descriptor-set-layout-bindings 1)
                  (.binding 1)
                  (.descriptorCount 1)
                  (.descriptorType VK13/VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                  (.pImmutableSamplers nil)
                  (.stageFlags VK13/VK_SHADER_STAGE_FRAGMENT_BIT)))
          descriptor-set-layout-create-info (doto (VkDescriptorSetLayoutCreateInfo/calloc stack)
                                              (.sType VK13/VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                                              (.pBindings descriptor-set-layout-bindings))
          descriptor-set-layout-ptr (.mallocLong stack 1)]
      (if (= (VK13/vkCreateDescriptorSetLayout (VulkanGlobals/getLogicalDevice) descriptor-set-layout-create-info nil descriptor-set-layout-ptr)
             VK13/VK_SUCCESS)
        (.set VulkanGlobals/DESCRIPTOR_SET_LAYOUT_POINTER (.get descriptor-set-layout-ptr 0))
        (throw (RuntimeException. "Failed to create descriptor set layout."))))))

(def buffer-size (+ (* #_number-of-matrix4f-fields 3 #_floats-per-matrix 16 Float/BYTES)
                    #_(* #_coords-per-vertex 2 #_vertices 4 #_xy-coord-sizeof Float/BYTES)))

(defn create-uniform-buffers []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [buffer-ptr* (.mallocLong stack 1)
          buffer-memory-ptr* (.mallocLong stack 1)
          uniform-buffers
          (repeatedly (.size (.get VulkanGlobals/SWAP_CHAIN_IMAGE_POINTERS))
                      (fn []
                        (buffer/create-buffer buffer-size VK13/VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT (util/bit-ors VK13/VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                                                                                                                VK13/VK_MEMORY_PROPERTY_HOST_COHERENT_BIT)
                                              buffer-ptr* buffer-memory-ptr* stack)
                        (doto (Buffer.)
                          (.bufferPointer (.get buffer-ptr* 0))
                          (.bufferMemoryPointer (.get buffer-memory-ptr* 0)))))]
      (.set VulkanGlobals/UNIFORM_BUFFERS (Vector. ^Collection uniform-buffers)))))

(defn update-uniform-buffer [current-frame-index ^MemoryStack stack]
  (let [model #_(Matrix4f.) (.rotate (Matrix4f.) (* (GLFW/glfwGetTime)
                                                  (Math/toRadians 90)) 0 0 1)
        view #_(Matrix4f.) (.lookAt (Matrix4f.) 0 -0.1 -1.5,, 0 0 0,, 0 0 1)
        proj (.perspective (Matrix4f.)
                           (float (Math/toRadians 45))
                           (float (/ (- (.width (.get VulkanGlobals/SWAP_CHAIN_EXTENT)))
                                     (.height (.get VulkanGlobals/SWAP_CHAIN_EXTENT))))
                           (float 0.1)
                           (float 10)
                           true)
        ubo (UniformBufferObject. model view proj)
        data-ptr* (.mallocPointer stack 1)]
    (buffer/staging-buffer-memcpy (.bufferMemoryPointer ^Buffer (.get VulkanGlobals/UNIFORM_BUFFERS current-frame-index))
                                  buffer-size data-ptr* ubo :buffer-copy/uniform-buffer-object)))

(defn create-descriptor-pool []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [descriptor-pool-sizes (VkDescriptorPoolSize/calloc 2 stack)
          _ (do (doto ^VkDescriptorPoolSize (.get descriptor-pool-sizes 0)
                  (.type VK13/VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                  (.descriptorCount (.size (.get VulkanGlobals/SWAP_CHAIN_IMAGE_POINTERS))))
                (doto ^VkDescriptorPoolSize (.get descriptor-pool-sizes 1)
                  (.type VK13/VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                  (.descriptorCount (.size (.get VulkanGlobals/SWAP_CHAIN_IMAGE_POINTERS)))))
          descriptor-pool-create-info (doto (VkDescriptorPoolCreateInfo/calloc stack)
                                        (.sType VK13/VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                                        (.pPoolSizes descriptor-pool-sizes)
                                        (.maxSets (.size (.get VulkanGlobals/SWAP_CHAIN_IMAGE_POINTERS))))
          descriptor-pool-ptr* (.mallocLong stack 1)]
      (if (= (VK13/vkCreateDescriptorPool (VulkanGlobals/getLogicalDevice) descriptor-pool-create-info nil descriptor-pool-ptr*)
             VK13/VK_SUCCESS)
        (.set VulkanGlobals/DESCRIPTOR_POOL_POINTER (.get descriptor-pool-ptr* 0))
        (throw (RuntimeException. "Failed to create descriptor pool."))))))

(defn create-descriptor-sets []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [descriptor-set-layouts-ptr (.mallocLong stack (.size (.get VulkanGlobals/SWAP_CHAIN_IMAGE_POINTERS)))
          _ (dotimes [i (.capacity descriptor-set-layouts-ptr)]
              (.put descriptor-set-layouts-ptr i (.get VulkanGlobals/DESCRIPTOR_SET_LAYOUT_POINTER)))
          descriptor-set-allocate-info (doto (VkDescriptorSetAllocateInfo/calloc stack)
                                         (.sType VK13/VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                                         (.descriptorPool (.get VulkanGlobals/DESCRIPTOR_POOL_POINTER))
                                         (.pSetLayouts descriptor-set-layouts-ptr))
          descriptor-sets-ptr (.mallocLong stack (.size (.get VulkanGlobals/SWAP_CHAIN_IMAGE_POINTERS)))
          _ (when (not= (VK13/vkAllocateDescriptorSets (VulkanGlobals/getLogicalDevice) descriptor-set-allocate-info descriptor-sets-ptr)
                        VK13/VK_SUCCESS)
              (throw (RuntimeException. "Failed to allocate descriptor sets.")))
          descriptor-buffer-info (doto (VkDescriptorBufferInfo/calloc 1 stack)
                                   (.offset 0)
                                   (.range buffer-size))
          image-info (doto (VkDescriptorImageInfo/calloc 1 stack)
                       (.imageLayout VK13/VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                       (.imageView (.textureImageViewPointer TEXTURE))
                       (.sampler (.textureSamplerPointer TEXTURE)))
          write-descriptor-sets (VkWriteDescriptorSet/calloc 2 stack)
          _ (do (doto ^VkWriteDescriptorSet (.get write-descriptor-sets 0)
                  (.sType VK13/VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                  (.dstBinding 0)
                  (.dstArrayElement 0)
                  (.descriptorType VK13/VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                  (.descriptorCount 1)
                  (.pBufferInfo descriptor-buffer-info)
                  (.pTexelBufferView nil))
                (doto ^VkWriteDescriptorSet (.get write-descriptor-sets 1)
                  (.sType VK13/VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                  (.dstBinding 1)
                  (.dstArrayElement 0)
                  (.descriptorType VK13/VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                  (.descriptorCount 1)
                  (.pImageInfo image-info)))
          descriptor-set-ptrs (mapv (fn [^Integer i]
                                      (let [descriptor-set (.get descriptor-sets-ptr i)]
                                        (.buffer descriptor-buffer-info (.bufferPointer ^Buffer (.get VulkanGlobals/UNIFORM_BUFFERS i)))
                                        (.dstSet ^VkWriteDescriptorSet (.get write-descriptor-sets 0) descriptor-set)
                                        (.dstSet ^VkWriteDescriptorSet (.get write-descriptor-sets 1) descriptor-set)
                                        (VK13/vkUpdateDescriptorSets (VulkanGlobals/getLogicalDevice) write-descriptor-sets nil)
                                        descriptor-set))
                                    (range (.size (.get VulkanGlobals/SWAP_CHAIN_IMAGE_POINTERS))))]
      (.set VulkanGlobals/DESCRIPTOR_SET_POINTERS (VulkanGlobalsInterfaces$VkPointerVector/asVkPointerVector (Vector. ^Collection descriptor-set-ptrs))))))