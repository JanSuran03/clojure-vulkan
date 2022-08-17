(ns clojure-vulkan.instance
  (:require [clojure-vulkan.debug :as debug]
            [clojure-vulkan.globals :as globals :refer [VULKAN-INSTANCE]]
            [clojure-vulkan.util :as util]
            [clojure-vulkan.validation-layers :as validation-layers])
  (:import (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK13 VkApplicationInfo VkDebugUtilsMessengerCreateInfoEXT VkInstance VkInstanceCreateInfo)))

(defn create []
  (when validation-layers/*enable-validation-layers*
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
                                              (.ppEnabledExtensionNames (validation-layers/get-required-extensions stack))
                                              (.ppEnabledLayerNames nil))
          ^VkInstanceCreateInfo create-info (if validation-layers/*enable-validation-layers*
                                              (let [^VkDebugUtilsMessengerCreateInfoEXT debug-create-info
                                                    (debug/init-debug-messenger-create-info
                                                      (VkDebugUtilsMessengerCreateInfoEXT/calloc stack))]
                                                (doto create-info
                                                  (.ppEnabledLayerNames (util/string-seq-as-pointer-buffer stack validation-layers/*validation-layers*))
                                                  (.pNext (.address debug-create-info)))))
          instance-ptr (.mallocPointer stack 1)]
      (when (not= (VK13/vkCreateInstance create-info nil instance-ptr) VK13/VK_SUCCESS)
        (throw (RuntimeException. "Failed to create Vulkan instance.")))
      (globals/set-global! VULKAN-INSTANCE (VkInstance. (.get instance-ptr 0) create-info)))))

(defn destroy-instance []
  (util/assert-not-null VULKAN-INSTANCE (VK13/vkDestroyInstance VULKAN-INSTANCE nil))
  (globals/reset-vulkan-instance))