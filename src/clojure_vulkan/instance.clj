(ns clojure-vulkan.instance
  (:require [clojure-vulkan.util :as util]
            [clojure-vulkan.validation-layers :as validation-layers])
  (:import (org.lwjgl.glfw GLFWVulkan)
           (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK13 VkApplicationInfo VkInstance VkInstanceCreateInfo)))

(def ^VkInstance vulkan-instance nil)

(defn create []
  (when validation-layers/*check-validation-layers*
    (validation-layers/check-validation-layers-support))
  (util/with-memory-stack-push ^MemoryStack stack
                               (let [^VkApplicationInfo app-info (doto (VkApplicationInfo/calloc stack)
                                        (.sType VK13/VK_STRUCTURE_TYPE_APPLICATION_INFO)
                                        (.pApplicationName (.UTF8Safe stack "Hello Vulkan app"))
                                        (.applicationVersion util/vk-version)
                                        (.pEngineName (.UTF8Safe stack "No engine name"))
                                        (.engineVersion util/vk-version)
                                        (.apiVersion VK13/VK_API_VERSION_1_3))
          ^VkInstanceCreateInfo create-info (doto (VkInstanceCreateInfo/calloc stack)
                                              (.sType VK13/VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                                              (.pApplicationInfo app-info)
                                              (.ppEnabledExtensionNames (validation-layers/get-required-extensions))
                                              (.ppEnabledLayerNames nil))
          ^VkInstanceCreateInfo create-info (if validation-layers/*check-validation-layers*
                                              (doto create-info
                                                (.ppEnabledLayerNames (validation-layers/validation-layers-as-pointer-buffer))))
          instance-ptr (.mallocPointer stack 1)]
      (when (not= (VK13/vkCreateInstance create-info nil instance-ptr) VK13/VK_SUCCESS)
        (throw (RuntimeException. "Failed to create Vulkan instance.")))
      (alter-var-root #'vulkan-instance
                      (constantly (VkInstance. (.get instance-ptr 0) create-info))))))

(defn destroy []
  (util/assert-not-null vulkan-instance
    (VK13/vkDestroyInstance vulkan-instance nil)))