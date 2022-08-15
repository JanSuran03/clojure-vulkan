(ns clojure-vulkan.render
  (:require [clojure-vulkan.globals :as globals :refer [LOGICAL-DEVICE SEMAPHORES-AND-FENCES]]
            [clojure-vulkan.util :as util])
  (:import (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VkSemaphoreCreateInfo VK13 VkFenceCreateInfo)))

(def max-frames-in-flight 2)

(defn create-sync-objects []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [in-flight-frames []
          images-in-flight {}
          semaphore-create-info (doto (VkSemaphoreCreateInfo/calloc stack)
                                  (.sType VK13/VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO))
          fence-create-info (doto (VkFenceCreateInfo/calloc stack)
                              (.sType VK13/VK_STRUCTURE_TYPE_FENCE_CREATE_INFO))
          image-available-semaphore-ptr (.mallocLong stack 1)
          render-finished-semaphore-ptr (.mallocLong stack 1)
          in-flight-fence-ptr (.mallocLong stack 1)]
      (when (or (not= (VK13/vkCreateSemaphore LOGICAL-DEVICE semaphore-create-info nil image-available-semaphore-ptr)
                      VK13/VK_SUCCESS)
                (not= (VK13/vkCreateSemaphore LOGICAL-DEVICE semaphore-create-info nil render-finished-semaphore-ptr)
                      VK13/VK_SUCCESS))
        (throw (RuntimeException. "Failed to create semaphores.")))
      (when (not= (VK13/vkCreateFence LOGICAL-DEVICE fence-create-info nil in-flight-fence-ptr)
                  VK13/VK_SUCCESS)
        (throw (RuntimeException. "Failed to create fence."))))))

(defn destroy-semaphores-and-fences []
  (let [{:keys [image-available-semaphore-pointer
                render-finished-semaphore-pointer
                in-flight-fence-pointer]} SEMAPHORES-AND-FENCES]
    (VK13/vkDestroySemaphore LOGICAL-DEVICE image-available-semaphore-pointer nil)
    (VK13/vkDestroySemaphore LOGICAL-DEVICE render-finished-semaphore-pointer nil)
    (VK13/vkDestroyFence LOGICAL-DEVICE in-flight-fence-pointer nil)
    (globals/reset-semaphores-and-fences)))

(defn draw-frame []
  )