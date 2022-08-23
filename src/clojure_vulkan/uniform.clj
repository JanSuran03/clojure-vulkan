(ns clojure-vulkan.uniform
  (:require [clojure-vulkan.buffer :as buffer]
            [clojure-vulkan.frame :as frame]
            [clojure-vulkan.globals :as globals :refer [DESCRIPTOR-SET-LAYOUT-POINTER LOGICAL-DEVICE SWAP-CHAIN-EXTENT SWAP-CHAIN-IMAGES
                                                        UNIFORM-BUFFER-POINTERS UNIFORM-BUFFER-MEMORY-POINTERS]]
            [clojure-vulkan.math.vertex :as vertex]
            [clojure-vulkan.util :as util])
  (:import (org.joml Matrix4f Vector3f)
           (org.lwjgl.glfw GLFW)
           (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK13 VkDescriptorSetLayoutCreateInfo VkDescriptorSetLayoutBinding VkExtent2D)
           (clojure_vulkan MemoryUtils UniformBufferObject)))

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

(def buffer-size (* #_num-matrix4f-fields 3 #_floats-per-matrix 16 Float/BYTES))

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
  (dotimes [i frame/MAX-FRAMES-IN-FLIGHT]
    (VK13/vkDestroyBuffer LOGICAL-DEVICE ^long (nth UNIFORM-BUFFER-POINTERS i) nil)
    (VK13/vkFreeMemory LOGICAL-DEVICE ^long (nth UNIFORM-BUFFER-MEMORY-POINTERS i) nil))
  (globals/reset-uniform-buffer-ptrs)
  (globals/reset-uniform-buffer-memory-ptrs))

(defn update-uniform-buffer [current-frame-index ^MemoryStack stack]
  (let [model (.rotateX (Matrix4f.) (GLFW/glfwGetTime))
        view (.lookAt (Matrix4f.) (Vector3f. 2 2 2) (Vector3f. 0 0 0) (Vector3f. 0 0 1))
        proj (.perspective (Matrix4f.)
                           (float (Math/toRadians 45))
                           (float (/ (.width ^VkExtent2D SWAP-CHAIN-EXTENT)
                                     (.height ^VkExtent2D SWAP-CHAIN-EXTENT)))
                           (float 0.1)
                           (float 10)
                           true)
        ubo (UniformBufferObject. model view proj)
        data-ptr (.mallocPointer stack 1)]
    (VK13/vkMapMemory LOGICAL-DEVICE (nth UNIFORM-BUFFER-MEMORY-POINTERS current-frame-index) #_offset 0 buffer-size #_flags 0 data-ptr)
    (MemoryUtils/memcpyUBO (.getByteBuffer data-ptr 0 buffer-size) ubo)
    (VK13/vkUnmapMemory LOGICAL-DEVICE (nth UNIFORM-BUFFER-MEMORY-POINTERS current-frame-index))))