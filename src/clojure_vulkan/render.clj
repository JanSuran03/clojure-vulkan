(ns clojure-vulkan.render
  (:require [clojure-vulkan.globals :as globals :refer [LOGICAL-DEVICE SEMAPHORES-AND-FENCES SWAP-CHAIN-POINTER]]
            [clojure-vulkan.util :as util])
  (:import (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VkSemaphoreCreateInfo VK13 VkFenceCreateInfo KHRSwapchain)))

(def max-frames-in-flight 2)
(def ^Long infinite-timeout 0x7fffffffffffffff)

(defn create-sync-objects []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [in-flight-frames []
          images-in-flight {}
          semaphore-create-info (doto (VkSemaphoreCreateInfo/calloc stack)
                                  (.sType VK13/VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO))
          fence-create-info (doto (VkFenceCreateInfo/calloc stack)
                              (.sType VK13/VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                              ; fence is created signaled -> a solution for the very first frame which cannot wait for the previous one
                              (.flags VK13/VK_FENCE_CREATE_SIGNALED_BIT))
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
        (throw (RuntimeException. "Failed to create fence.")))
      (globals/set-global! SEMAPHORES-AND-FENCES {:image-available-semaphore-pointer (.get image-available-semaphore-ptr 0)
                                                  :render-finished-semaphore-pointer (.get render-finished-semaphore-ptr 0)
                                                  :in-flight-fence-pointer           (.get in-flight-fence-ptr 0)}))))

(defn destroy-semaphores-and-fences []
  (let [{:keys [image-available-semaphore-pointer
                render-finished-semaphore-pointer
                in-flight-fence-pointer]} SEMAPHORES-AND-FENCES]
    (VK13/vkDestroySemaphore LOGICAL-DEVICE image-available-semaphore-pointer nil)
    (VK13/vkDestroySemaphore LOGICAL-DEVICE render-finished-semaphore-pointer nil)
    (VK13/vkDestroyFence LOGICAL-DEVICE in-flight-fence-pointer nil)
    (globals/reset-semaphores-and-fences)))

(defn draw-frame []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [image-index (.mallocInt stack 1)]
      (KHRSwapchain/vkAcquireNextImageKHR LOGICAL-DEVICE SWAP-CHAIN-POINTER infinite-timeout
                                          ^Long (:image-available-semaphore-pointer SEMAPHORES-AND-FENCES)
                                          VK13/VK_NULL_HANDLE image-index)
      (VK13/vkWaitForFences LOGICAL-DEVICE ^Long (:in-flight-fence-pointer SEMAPHORES-AND-FENCES) true infinite-timeout)
      (VK13/vkResetFences LOGICAL-DEVICE ^Long (:in-flight-fence-pointer SEMAPHORES-AND-FENCES)))))