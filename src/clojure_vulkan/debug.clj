(ns clojure-vulkan.debug
  (:require [clojure-vulkan.validation-layers :as validation-layers]
            [clojure-vulkan.util :as util :refer [nullptr]])
  (:import (org.lwjgl.vulkan VkDebugUtilsMessengerCallbackDataEXT VkDebugUtilsMessengerCreateInfoEXT VK13 EXTDebugUtils VkDebugUtilsMessengerCallbackEXTI)
           (org.lwjgl.system MemoryStack)))

(def ^long debug-messenger)

(def debug-callback
  (reify VkDebugUtilsMessengerCallbackEXTI
    (invoke [this message-severity message-type callback-data-ptr user-data-ptr]
      (let [^VkDebugUtilsMessengerCallbackDataEXT callback-data (VkDebugUtilsMessengerCallbackDataEXT/create ^long callback-data-ptr)]
        (binding [*out* *err*]
          (println "Validation layer callback: " (.pMessageString callback-data)))))))

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
              (.pUserData nullptr))]))))