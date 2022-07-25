(ns clojure-vulkan.debug
  (:require [clojure-vulkan.validation-layers :as validation-layers]
            [clojure-vulkan.util :as util :refer [nullptr]]
            [clojure-vulkan.instance :as instance])
  (:import (org.lwjgl.vulkan VkDebugUtilsMessengerCallbackDataEXT VkDebugUtilsMessengerCreateInfoEXT VK13 EXTDebugUtils VkDebugUtilsMessengerCallbackEXTI VkInstance VkAllocationCallbacks)
           (org.lwjgl.system MemoryStack)
           (java.nio LongBuffer)))

(def ^long debug-messenger -1)

(def debug-callback
  (reify VkDebugUtilsMessengerCallbackEXTI
    (invoke [this message-severity message-type callback-data-ptr user-data-ptr]
      (let [^VkDebugUtilsMessengerCallbackDataEXT callback-data (VkDebugUtilsMessengerCallbackDataEXT/create ^long callback-data-ptr)]
        (binding [*out* *err*]
          (println "Validation layer callback: " (.pMessageString callback-data))))
      false)))

(defn create-debug-messenger-extension [^VkInstance instance ^VkDebugUtilsMessengerCreateInfoEXT create-info
                                        ^VkAllocationCallbacks allocation-callbacks ^LongBuffer debug-messenger-ptr]
  (if (= (VK13/vkGetInstanceProcAddr instance "vkCreateDebugUtilsMessengerEXT") nullptr)
    false
    (EXTDebugUtils/vkCreateDebugUtilsMessengerEXT instance create-info allocation-callbacks debug-messenger-ptr)))

(defn setup-debug-messenger []
  (when validation-layers/*enable-validation-layers*
    (util/with-memory-stack-push ^MemoryStack stack
      (let [^VkDebugUtilsMessengerCreateInfoEXT create-info
            (doto (VkDebugUtilsMessengerCreateInfoEXT/calloc stack)
              (.sType EXTDebugUtils/VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
              (.messageSeverity (util/bit-ors EXTDebugUtils/VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT
                                              EXTDebugUtils/VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT
                                              EXTDebugUtils/VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT))
              (.messageType (util/bit-ors EXTDebugUtils/VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT
                                          EXTDebugUtils/VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT
                                          EXTDebugUtils/VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT))
              (.pfnUserCallback debug-callback)
              (.pUserData nullptr))
            ^LongBuffer debug-messenger-ptr (.longs stack VK13/VK_NULL_HANDLE)]
        (if (create-debug-messenger-extension instance/vulkan-instance create-info nil debug-messenger-ptr)
          (alter-var-root #'debug-messenger (constantly (.get debug-messenger-ptr 0)))
          (throw (RuntimeException. "Failed to set up debug messenger.")))))))