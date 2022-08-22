(ns clojure-vulkan.validation-layers
  (:require [clojure.string :as str]
            [clojure-vulkan.util :as util])
  (:import (org.lwjgl PointerBuffer)
           (org.lwjgl.glfw GLFWVulkan)
           (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan EXTDebugUtils VK13 VkLayerProperties VkLayerProperties$Buffer)))

(def ^:dynamic *enable-validation-layers* true)
(def ^:dynamic *validation-layers* #{"VK_LAYER_KHRONOS_validation"})

(defn check-validation-layers-support []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [layer-count (.ints stack 0)
          _ (VK13/vkEnumerateInstanceLayerProperties layer-count nil)
          ^VkLayerProperties$Buffer available-layers (VkLayerProperties/malloc (.get layer-count 0) stack)
          _ (VK13/vkEnumerateInstanceLayerProperties layer-count available-layers)
          available-layers (->> available-layers util/struct-buffer->seq
                                (map (memfn ^VkLayerProperties layerNameString)) set)]
      (when-not (every? #(contains? available-layers %) *validation-layers*)
        (throw (RuntimeException. (str "Some of validation layers are not supported."
                                       "\nAvailable layers: " (str/join ", " available-layers)
                                       "\nRequired layers: " (str/join ", " *validation-layers*))))))))

(defn get-required-extensions [^MemoryStack stack]
  (let [^PointerBuffer glfw-extensions (or (GLFWVulkan/glfwGetRequiredInstanceExtensions)
                                           (throw (RuntimeException. (str "Required Vulkan instance extensions couldn't be "
                                                                          "queried - your graphics card doesn't support Vulkan."))))]
    (if *enable-validation-layers*
      (doto (.mallocPointer stack (inc (.capacity glfw-extensions)))
        (.put glfw-extensions)
        (.put (.UTF8 stack EXTDebugUtils/VK_EXT_DEBUG_UTILS_EXTENSION_NAME))
        .rewind)
      glfw-extensions)))
