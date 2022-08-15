(ns clojure-vulkan.command-buffers
  (:require [clojure-vulkan.globals :as globals :refer [COMMAND-POOL-POINTER LOGICAL-DEVICE QUEUE-FAMILIES]]
            [clojure-vulkan.util :as util])
  (:import (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK13 VkCommandPoolCreateInfo)))

(defn create-command-pool []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [command-pool-create-info (doto (VkCommandPoolCreateInfo/calloc stack)
                                     (.sType VK13/VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                                     (.flags VK13/VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                                     (.queueFamilyIndex (:graphics-family QUEUE-FAMILIES)))
          command-pool-ptr (.mallocLong stack 1)]

      (if (= (VK13/vkCreateCommandPool LOGICAL-DEVICE command-pool-create-info nil command-pool-ptr)
             VK13/VK_SUCCESS)
        (globals/set-global! COMMAND-POOL-POINTER (.get command-pool-ptr 0))
        (throw (RuntimeException. "Failed to create command pool."))))))

(defn destroy-command-pool []
  (VK13/vkDestroyCommandPool LOGICAL-DEVICE COMMAND-POOL-POINTER nil)
  (globals/reset-command-pool-ptr))