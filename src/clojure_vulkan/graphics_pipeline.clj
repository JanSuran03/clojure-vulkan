(ns clojure-vulkan.graphics-pipeline
  (:require [clojure-vulkan.globals :refer [LOGICAL-DEVICE SWAP-CHAIN-EXTENT]]
            [clojure-vulkan.shaders :as shaders]
            [clojure-vulkan.util :as util])
  (:import (clojure_vulkan.shaders SpirVShader)
           (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK13 VkPipelineDynamicStateCreateInfo VkPipelineShaderStageCreateInfo
                             VkPipelineShaderStageCreateInfo$Buffer VkPipelineVertexInputStateCreateInfo VkShaderModuleCreateInfo VkPipelineInputAssemblyStateCreateInfo VkViewport VkExtent2D)))

(def ^:private dynamic-states-vec [VK13/VK_DYNAMIC_STATE_VIEWPORT VK13/VK_DYNAMIC_STATE_SCISSOR])

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
    (let [vertex-shader-in-spir-v-format (shaders/compile-shader "shader.vert" :shader-type/vertex)
          fragment-shader-in-spir-v-format (shaders/compile-shader "shader.frag" :shader-type/fragment)
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
                                              (.pName entry-point))
          dynamic-state-create-info (doto (VkPipelineDynamicStateCreateInfo/calloc stack)
                                      (.sType VK13/VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
                                      (.pDynamicStates (util/integers-as-pointer-buffer stack dynamic-states-vec)))
          vertex-input-state-create-info (doto (VkPipelineVertexInputStateCreateInfo/calloc stack)
                                           (.sType VK13/VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                                           (.pVertexBindingDescriptions nil)
                                           (.pVertexAttributeDescriptions nil))
          input-assembly-state-create-info (doto (VkPipelineInputAssemblyStateCreateInfo/calloc stack)
                                             (.sType VK13/VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                                             (.topology VK13/VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST) ; triangle from every 3 new vertices without reuse
                                             (.primitiveRestartEnable false))
          viewport (doto (VkViewport/calloc stack)
                     (.x 0)
                     (.y 0)
                     (.width (float (.width ^VkExtent2D SWAP-CHAIN-EXTENT)))
                     (.height (float (.height ^VkExtent2D SWAP-CHAIN-EXTENT)))
                     (.minDepth (float 0))
                     (.maxDepth (float 0)))]
      (VK13/vkDestroyShaderModule LOGICAL-DEVICE vertex-shader-module nil)
      (VK13/vkDestroyShaderModule LOGICAL-DEVICE fragment-shader-module nil)
      (.free ^SpirVShader vertex-shader-in-spir-v-format)
      (.free ^SpirVShader fragment-shader-in-spir-v-format))))