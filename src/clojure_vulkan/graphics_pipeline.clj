(ns clojure-vulkan.graphics-pipeline
  (:require [clojure-vulkan.shaders :as shaders]
            [clojure-vulkan.util :as util])
  (:import (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VkShaderModuleCreateInfo VK13)))

(defn- create-shader-module [shader-bytecode]
  (util/with-memory-stack-push ^MemoryStack stack
    (let [create-info (doto (VkShaderModuleCreateInfo/calloc stack)
                        (.sType VK13/VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                        (.pCode))])))

(defn create-graphics-pipeline []
  (let [;vertex-shader-bytecode (shaders/get-shader-in-spir-v-bytecode "shader.vert" "shader-vert.spv")
        ;fragment-shader-bytecode (shaders/get-shader-in-spir-v-bytecode "shader.frag" "shader-frag.spv")
        vertex-shader-bytebuffer (shaders/compile-shader "shaders.vert" :shader-type/vertex)
        fragment-shader-bytebuffer (shaders/compile-shader "shaders.frag" :shader-type/fragment)]))