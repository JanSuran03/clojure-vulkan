(ns clojure-vulkan.logical-device-and-queue
  (:require [clojure-vulkan.globals :refer [graphics-queue logical-device physical-device queue-families]]
            [clojure-vulkan.util :as util]
            [clojure-vulkan.validation-layers :as validation-layers])
  (:import (org.lwjgl.vulkan VK13 VkDevice VkDeviceCreateInfo VkDeviceQueueCreateInfo VkDeviceQueueCreateInfo$Buffer
                             VkPhysicalDeviceFeatures)
           (org.lwjgl.system MemoryStack)))

(defn create-logical-device []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [{:keys [graphics-family present-family]} queue-families
          unique-queue-families (hash-set graphics-family present-family)
          ^VkDeviceQueueCreateInfo$Buffer queue-create-infos (VkDeviceQueueCreateInfo/calloc (count unique-queue-families) stack)
          _ (doseq [[ i queue-family] (map-indexed (fn [i family] [i family]) unique-queue-families)]
              (doto ^VkDeviceQueueCreateInfo (.get queue-create-infos ^Long i)
                (.sType VK13/VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                (.queueFamilyIndex queue-family)
                (.pQueuePriorities (.floats stack (float 1.0)))))
          ^VkPhysicalDeviceFeatures device-features (VkPhysicalDeviceFeatures/calloc stack)
          ^VkDeviceCreateInfo device-create-info (doto (VkDeviceCreateInfo/calloc stack)
                                                   (.sType VK13/VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                                                   (.pQueueCreateInfos queue-create-infos)
                                                   (.pEnabledFeatures device-features))
          ^VkDeviceCreateInfo device-create-info (cond-> device-create-info
                                                         validation-layers/*enable-validation-layers*
                                                         (.ppEnabledLayerNames (validation-layers/validation-layers-as-pointer-buffer)))
          device-ptr (.pointers stack VK13/VK_NULL_HANDLE)
          _ (when (not= (VK13/vkCreateDevice physical-device device-create-info nil device-ptr)
                        VK13/VK_SUCCESS)
              (throw (RuntimeException. "Failed to create a logical device.")))
          device (VkDevice. (.get device-ptr 0) physical-device device-create-info)
          graphics-queue-ptr (.pointers stack VK13/VK_NULL_HANDLE)
          present-queue-ptr (.pointers stack VK13/VK_NULL_HANDLE)]
      (VK13/vkGetDeviceQueue device graphics-family 0 graphics-queue-ptr)
      (VK13/vkGetDeviceQueue device present-family 0 present-queue-ptr)
      (alter-var-root #'logical-device (constantly device))
      (alter-var-root #'graphics-queue (constantly (.get graphics-queue-ptr 0))))))
