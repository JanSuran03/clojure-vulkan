(ns clojure-vulkan.uniform
  (:require [clojure-vulkan.buffer :as buffer]
            [clojure-vulkan.globals :as globals :refer [DESCRIPTOR-POOL-POINTER DESCRIPTOR-SET-LAYOUT-POINTER DESCRIPTOR-SET-POINTERS LOGICAL-DEVICE SWAP-CHAIN-EXTENT
                                                        SWAP-CHAIN-IMAGES UNIFORM-BUFFER-POINTERS UNIFORM-BUFFER-MEMORY-POINTERS]]
            [clojure-vulkan.math.vertex :as vertex]
            [clojure-vulkan.util :as util])
  (:import (clojure_vulkan Mat4f MemoryUtils UniformBufferObject)
           (org.joml Matrix4f)
           (org.lwjgl.glfw GLFW)
           (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK13 VkDescriptorPoolCreateInfo VkDescriptorPoolSize VkDescriptorSetLayoutBinding VkDescriptorSetLayoutCreateInfo VkExtent2D VkDescriptorSetAllocateInfo VkDescriptorBufferInfo VkWriteDescriptorSet)))

(defn create-descriptor-set-layout []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [descriptor-set-layout-bindings (doto (VkDescriptorSetLayoutBinding/calloc 1 stack)
                                           (.binding ^int (get-in vertex/current-triangle-vbo-characterictics [:uniform 0 :binding]))
                                           (.descriptorType VK13/VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                                           (.descriptorCount 1) ; the shader variable can represent an array of UBOs
                                           (.stageFlags VK13/VK_SHADER_STAGE_VERTEX_BIT)
                                           (.pImmutableSamplers nil))
          descriptor-set-layout-create-info (doto (VkDescriptorSetLayoutCreateInfo/calloc stack)
                                              (.sType VK13/VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                                              (.pBindings descriptor-set-layout-bindings))
          descriptor-set-layout-ptr (.mallocLong stack 1)]
      (if (= (VK13/vkCreateDescriptorSetLayout LOGICAL-DEVICE descriptor-set-layout-create-info nil descriptor-set-layout-ptr)
             VK13/VK_SUCCESS)
        (globals/set-global! DESCRIPTOR-SET-LAYOUT-POINTER (.get descriptor-set-layout-ptr 0))
        (throw (RuntimeException. "Failed to create descriptor set layout."))))))

(defn destroy-descriptor-set-layout []
  (VK13/vkDestroyDescriptorSetLayout LOGICAL-DEVICE DESCRIPTOR-SET-LAYOUT-POINTER nil)
  (globals/reset-descriptor-set-layout-ptr))

(def buffer-size (* #_number-of-matrix4f-fields 3 #_floats-per-matrix 16 Float/BYTES))

(defn create-uniform-buffers []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [buffer-ptr* (.mallocLong stack 1)
          buffer-memory-ptr* (.mallocLong stack 1)
          [uniform-buffer-ptrs uniform-buffer-memory-ptrs]
          (apply util/nths
                 (repeatedly (count SWAP-CHAIN-IMAGES)
                             (fn []
                               (buffer/create-buffer buffer-size VK13/VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT (util/bit-ors VK13/VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                                                                                                                       VK13/VK_MEMORY_PROPERTY_HOST_COHERENT_BIT)
                                                     buffer-ptr* buffer-memory-ptr* stack)
                               [(.get buffer-ptr* 0) (.get buffer-memory-ptr* 0)])))]
      (globals/set-global! UNIFORM-BUFFER-POINTERS uniform-buffer-ptrs)
      (globals/set-global! UNIFORM-BUFFER-MEMORY-POINTERS uniform-buffer-memory-ptrs))))

(defn destroy-uniform-buffers []
  (dotimes [i (count SWAP-CHAIN-IMAGES)]
    (VK13/vkDestroyBuffer LOGICAL-DEVICE ^long (nth UNIFORM-BUFFER-POINTERS i) nil)
    (VK13/vkFreeMemory LOGICAL-DEVICE ^long (nth UNIFORM-BUFFER-MEMORY-POINTERS i) nil))
  (globals/reset-uniform-buffer-ptrs)
  (globals/reset-uniform-buffer-memory-ptrs))

(defn update-uniform-buffer [current-frame-index ^MemoryStack stack]
  (let [model (.rotate (Matrix4f.) (* (GLFW/glfwGetTime)
                                      (Math/toRadians 90)) 0 0 1)
        view (.lookAt (Matrix4f.) 2 2 2,, 0 0 0,, 0 0 1)
        proj (.perspective (Matrix4f.)
                           (float (Math/toRadians 45))
                           (float (/ (- (.width ^VkExtent2D SWAP-CHAIN-EXTENT))
                                     (.height ^VkExtent2D SWAP-CHAIN-EXTENT)))
                           (float 0.1)
                           (float 10)
                           true)
        ubo (UniformBufferObject. model view proj)
        data-ptr (.mallocPointer stack 1)]
    (VK13/vkMapMemory LOGICAL-DEVICE (nth UNIFORM-BUFFER-MEMORY-POINTERS current-frame-index) #_offset 0 buffer-size #_flags 0 data-ptr)
    (MemoryUtils/memcpyUBO (.getByteBuffer data-ptr 0 buffer-size) ubo)
    (VK13/vkUnmapMemory LOGICAL-DEVICE (nth UNIFORM-BUFFER-MEMORY-POINTERS current-frame-index))))

(defn create-descriptor-pool []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [descriptor-pool-size (doto (VkDescriptorPoolSize/calloc 1 stack)
                                 (.type VK13/VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                                 (.descriptorCount (count SWAP-CHAIN-IMAGES)))
          descriptor-pool-create-info (doto (VkDescriptorPoolCreateInfo/calloc stack)
                                        (.sType VK13/VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                                        (.pPoolSizes descriptor-pool-size)
                                        (.maxSets (count SWAP-CHAIN-IMAGES)))
          descriptor-pool-ptr (.mallocLong stack 1)]
      (if (= (VK13/vkCreateDescriptorPool LOGICAL-DEVICE descriptor-pool-create-info nil descriptor-pool-ptr)
             VK13/VK_SUCCESS)
        (globals/set-global! DESCRIPTOR-POOL-POINTER (.get descriptor-pool-ptr 0))
        (throw (RuntimeException. "Failed to create descriptor pool."))))))

(defn destroy-descriptor-pool []
  (VK13/vkDestroyDescriptorPool LOGICAL-DEVICE DESCRIPTOR-POOL-POINTER nil)
  (globals/reset-descriptor-pool-ptr))

(defn create-descriptor-sets []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [descriptor-set-layouts-ptr (.mallocLong stack (count SWAP-CHAIN-IMAGES))
          _ (dotimes [i (.capacity descriptor-set-layouts-ptr)]
              (.put descriptor-set-layouts-ptr i DESCRIPTOR-SET-LAYOUT-POINTER))
          descriptor-set-allocate-info (doto (VkDescriptorSetAllocateInfo/calloc stack)
                                         (.sType VK13/VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                                         (.descriptorPool DESCRIPTOR-POOL-POINTER)
                                         (.pSetLayouts descriptor-set-layouts-ptr))
          descriptor-sets-ptr (.mallocLong stack (count SWAP-CHAIN-IMAGES))
          _ (if (= (VK13/vkAllocateDescriptorSets LOGICAL-DEVICE descriptor-set-allocate-info descriptor-sets-ptr)
                   VK13/VK_SUCCESS)
              (globals/set-global! DESCRIPTOR-SET-POINTERS [])
              (throw (RuntimeException. "Failed to allocate descriptor sets.")))
          descriptor-buffer-info (doto (VkDescriptorBufferInfo/calloc 1 stack)
                                   (.offset 0)
                                   (.range buffer-size))
          write-descriptor-set (doto (VkWriteDescriptorSet/calloc 1 stack)
                                 (.sType VK13/VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                                 (.dstBinding 0)
                                 (.dstArrayElement 0)
                                 (.descriptorType VK13/VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                                 (.descriptorCount 1)
                                 (.pBufferInfo descriptor-buffer-info)
                                 (.pImageInfo nil)
                                 (.pTexelBufferView nil))
          descriptor-set-ptrs (mapv (fn [i]
                                      (.buffer descriptor-buffer-info (nth UNIFORM-BUFFER-POINTERS i))
                                      (.dstSet write-descriptor-set (.get descriptor-sets-ptr ^int i))
                                      (VK13/vkUpdateDescriptorSets LOGICAL-DEVICE write-descriptor-set nil)
                                      (.get descriptor-sets-ptr))
                                    (range (count SWAP-CHAIN-IMAGES)))]
      (globals/set-global! DESCRIPTOR-SET-POINTERS descriptor-set-ptrs))))