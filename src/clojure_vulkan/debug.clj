(ns clojure-vulkan.debug
  (:require [clojure-vulkan.globals :as globals :refer [DEBUG-MESSENGER-POINTER VULKAN-INSTANCE]]
            [clojure-vulkan.util :as util]
            [clojure-vulkan.validation-layers :as validation-layers])
  (:import (java.nio LongBuffer)
           (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan EXTDebugUtils VK13 VkAllocationCallbacks VkDebugUtilsMessengerCallbackDataEXT VkDebugUtilsMessengerCallbackEXTI
                             VkDebugUtilsMessengerCreateInfoEXT VkInstance)))

(def debug-callback
  (reify VkDebugUtilsMessengerCallbackEXTI
    (invoke [this message-severity message-type callback-data-ptr user-data-ptr]
      (let [^VkDebugUtilsMessengerCallbackDataEXT callback-data (VkDebugUtilsMessengerCallbackDataEXT/create ^long callback-data-ptr)
            message-severity (case message-severity
                               0x1 "VERBOSE"
                               0x10 "INFO"
                               0x100 "WARNING"
                               0x1000 "ERROR")]
        (if util/*current-debug-filename*
          (spit util/*current-debug-filename*
                (str "Validation layer callback:\n"
                     (.pMessageString callback-data)
                     \newline)
                :append true)
          (binding [*out* *err*]
            (println (str "Validation layer callback (severity = " message-severity "): "
                          (.pMessageString callback-data)))))
        (case message-severity
          "ERROR" (throw (RuntimeException.
                           (str "\n\n>>>>> VALIDATION LAYER ERROR:\n"
                                (.pMessageString callback-data)
                                \newline \newline)))
          "WARNING" (binding [*out* *err*]
                      (println "Validation layer warning!\n"
                               "***********************************"))
          nil))
      VK13/VK_FALSE)))

(defn create-debug-messenger-extension [^VkInstance instance ^VkDebugUtilsMessengerCreateInfoEXT create-info
                                        ^VkAllocationCallbacks allocation-callbacks ^LongBuffer debug-messenger-ptr]
  (when (not= (VK13/vkGetInstanceProcAddr instance "vkCreateDebugUtilsMessengerEXT")
              VK13/VK_NULL_HANDLE)
    (= (EXTDebugUtils/vkCreateDebugUtilsMessengerEXT instance create-info allocation-callbacks debug-messenger-ptr)
       VK13/VK_SUCCESS)))

(defn init-debug-messenger-create-info [^VkDebugUtilsMessengerCreateInfoEXT create-info]
  (doto create-info
    (.sType EXTDebugUtils/VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
    (.messageSeverity (util/bit-ors EXTDebugUtils/VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT
                                    EXTDebugUtils/VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT
                                    EXTDebugUtils/VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT
                                    EXTDebugUtils/VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT))

    (.messageType (util/bit-ors EXTDebugUtils/VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT
                                EXTDebugUtils/VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT
                                EXTDebugUtils/VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT))
    (.pfnUserCallback debug-callback)
    (.pUserData VK13/VK_NULL_HANDLE)))

(defn setup-debug-messenger []
  (when validation-layers/*enable-validation-layers*
    (util/with-memory-stack-push ^MemoryStack stack
      (let [^VkDebugUtilsMessengerCreateInfoEXT create-info
            (init-debug-messenger-create-info (VkDebugUtilsMessengerCreateInfoEXT/calloc stack))
            ^LongBuffer debug-messenger-ptr (.longs stack VK13/VK_NULL_HANDLE)]
        (if (create-debug-messenger-extension VULKAN-INSTANCE create-info nil debug-messenger-ptr)
          (globals/set-global! DEBUG-MESSENGER-POINTER (.get debug-messenger-ptr 0))
          (throw (RuntimeException. "Failed to set up debug messenger.")))))))

(defn destroy-debug-messenger [^VkAllocationCallbacks allocation-callbacks]
  (when (not= (VK13/vkGetInstanceProcAddr VULKAN-INSTANCE "vkDestroyDebugUtilsMessengerEXT")
              VK13/VK_NULL_HANDLE)
    (EXTDebugUtils/vkDestroyDebugUtilsMessengerEXT VULKAN-INSTANCE DEBUG-MESSENGER-POINTER allocation-callbacks)
    (globals/reset-debug-messenger)))