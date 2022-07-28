(ns clojure-vulkan.logical-device-and-queue
  (:require [clojure-vulkan.globals :refer [graphics-queue logical-device physical-device queue-families]]
            [clojure-vulkan.util :as util]
            [clojure-vulkan.validation-layers :as validation-layers])
  (:import (org.lwjgl.vulkan VK13 VkDevice VkDeviceCreateInfo VkDeviceQueueCreateInfo VkPhysicalDeviceFeatures)
           (org.lwjgl.system MemoryStack)))

(defn create-logical-device []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [{:keys [graphics-family]} queue-families
          ^VkDeviceQueueCreateInfo queue-create-info (doto (VkDeviceQueueCreateInfo/calloc 1 stack)
                                                       (.sType VK13/VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                                                       (.queueFamilyIndex graphics-family)
                                                       (.pQueuePriorities (.floats stack (float 1.0))))
          ^VkPhysicalDeviceFeatures device-features (doto (VkPhysicalDeviceFeatures/calloc stack))
          ^VkDeviceCreateInfo device-create-info (doto (VkDeviceCreateInfo/calloc stack)
                                                   (.sType VK13/VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                                                   (.pQueueCreateInfos queue-create-info)
                                                   (.pEnabledFeatures device-features))
          ^VkDeviceCreateInfo device-create-info (if validation-layers/*enable-validation-layers*
                                                   (cond-> device-create-info
                                                           validation-layers/*enable-validation-layers*
                                                           (.ppEnabledLayerNames (validation-layers/validation-layers-as-pointer-buffer)))
                                                   device-create-info)
          device-ptr (.pointers stack VK13/VK_NULL_HANDLE)
          _ (when (not= (VK13/vkCreateDevice physical-device device-create-info nil device-ptr)
                        VK13/VK_SUCCESS)
              (throw (RuntimeException. "Failed to create a logical device.")))
          device (VkDevice. (.get device-ptr 0) physical-device device-create-info)
          graphics-queue-ptr (.pointers stack VK13/VK_NULL_HANDLE)
          _ (VK13/vkGetDeviceQueue device graphics-family 0 graphics-queue-ptr)]
      (alter-var-root #'logical-device (constantly device))
      (alter-var-root #'graphics-queue (constantly (.get graphics-queue-ptr 0))))))
