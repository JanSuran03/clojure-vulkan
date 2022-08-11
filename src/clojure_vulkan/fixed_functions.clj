(ns clojure-vulkan.fixed-functions
  (:require [clojure-vulkan.util :as util])
  (:import (org.lwjgl.vulkan VK13 VkPipelineDynamicStateCreateInfo)
           (org.lwjgl.system MemoryStack)
           (java.nio IntBuffer)))

(def dynamic-states-vec [VK13/VK_DYNAMIC_STATE_VIEWPORT VK13/VK_DYNAMIC_STATE_SCISSOR])

(defn dynamic-state []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [^IntBuffer dynamic-states-buffer (util/integers-as-pointer-buffer stack dynamic-states-vec)
          create-info (doto (VkPipelineDynamicStateCreateInfo/calloc stack)
                        (.sType VK13/VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
                        (.pDynamicStates dynamic-states-buffer))])))