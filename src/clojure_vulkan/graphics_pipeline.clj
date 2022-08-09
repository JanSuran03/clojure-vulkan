(ns clojure-vulkan.graphics-pipeline
  (:require [clojure-vulkan.shaders :as shaders]
            [clojure-vulkan.util :as util]
            [clojure-vulkan.globals :refer [LOGICAL-DEVICE]])
  (:import (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VkShaderModuleCreateInfo VK13 VkPipelineShaderStageCreateInfo VkPipelineShaderStageCreateInfo$Buffer)))

(defn- create-shader-module [shader-in-spir-v-format]
  (util/with-memory-stack-push ^MemoryStack stack
    (let [create-info (doto (VkShaderModuleCreateInfo/calloc stack)
                        (.sType VK13/VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                        (.pCode (shaders/get-bytebuffer shader-in-spir-v-format)))
          shader-module-ptr (.mallocLong stack 1)]
      (when (not= (VK13/vkCreateShaderModule LOGICAL-DEVICE create-info nil shader-module-ptr)
                  VK13/VK_SUCCESS)
        (throw (RuntimeException. "Failed to create shader module.")))
      (.get shader-module-ptr 0))))

(defn create-graphics-pipeline []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [;vertex-shader-bytecode (shaders/get-shader-in-spir-v-bytecode "shader.vert" "shader-vert.spv")
          ;fragment-shader-bytecode (shaders/get-shader-in-spir-v-bytecode "shader.frag" "shader-frag.spv")
          vertex-shader-in-spir-v-format (shaders/compile-shader "shaders.vert" :shader-type/vertex)
          fragment-shader-in-spir-v-format (shaders/compile-shader "shaders.frag" :shader-type/fragment)
          vertex-shader-module (create-shader-module vertex-shader-in-spir-v-format)
          fragment-shader-module (create-shader-module fragment-shader-in-spir-v-format)
          entry-point (.UTF8 stack "main")
          ^VkPipelineShaderStageCreateInfo$Buffer shader-stages (VkPipelineShaderStageCreateInfo/calloc 2 stack)
          vertex-shader-stage-create-info (doto ^VkPipelineShaderStageCreateInfo (.get shader-stages 0)
                                            (.sType VK13/VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                                            (.stage VK13/VK_SHADER_STAGE_VERTEX_BIT)
                                            (.module vertex-shader-module)
                                            (.pName entry-point))
          fragment-shader-stage-create-info (doto ^VkPipelineShaderStageCreateInfo (.get shader-stages 1)
                                              (.sType VK13/VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                                              (.stage VK13/VK_SHADER_STAGE_FRAGMENT_BIT)
                                              (.module fragment-shader-module)
                                              (.pName entry-point))]
      (VK13/vkDestroyShaderModule LOGICAL-DEVICE vertex-shader-module nil)
      (VK13/vkDestroyShaderModule LOGICAL-DEVICE fragment-shader-module nil)
      (.free vertex-shader-in-spir-v-format)
      (.free fragment-shader-in-spir-v-format))))