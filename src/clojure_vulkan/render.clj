(ns clojure-vulkan.render
  (:require
    [clojure-vulkan.frame :as frame :refer [FRAMES MAX-FRAMES-IN-FLIGHT]]
    [clojure-vulkan.globals :as globals :refer [COMMAND-BUFFERS GRAPHICS-QUEUE LOGICAL-DEVICE PRESENT-QUEUE SWAP-CHAIN-POINTER]]
    [clojure-vulkan.util :as util])
  (:import (org.lwjgl.system MemoryStack Pointer)
           (org.lwjgl.vulkan KHRSwapchain VK13 VkFenceCreateInfo VkPresentInfoKHR VkSemaphoreCreateInfo VkSubmitInfo)
           (clojure_vulkan.frame Frame)))

(def ^Long infinite-timeout 0x7fffffffffffffff)

;; ***********************************************************************************************************
;; ***********************************************************************************************************

(defn create-sync-objects []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [semaphore-create-info (doto (VkSemaphoreCreateInfo/calloc stack)
                                  (.sType VK13/VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO))
          fence-create-info (doto (VkFenceCreateInfo/calloc stack)
                              (.sType VK13/VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                              ; fence is created signaled -> a solution for the very first frame which cannot wait for the previous one
                              (.flags VK13/VK_FENCE_CREATE_SIGNALED_BIT))
          image-available-semaphore-ptr (.mallocLong stack MAX-FRAMES-IN-FLIGHT)
          render-finished-semaphore-ptr (.mallocLong stack MAX-FRAMES-IN-FLIGHT)
          in-flight-fence-ptr (.mallocLong stack MAX-FRAMES-IN-FLIGHT)]
      (globals/set-global! FRAMES
        (mapv (fn [^Integer i]
                (when (or (not= (VK13/vkCreateSemaphore LOGICAL-DEVICE semaphore-create-info nil image-available-semaphore-ptr)
                                VK13/VK_SUCCESS)
                          (not= (VK13/vkCreateSemaphore LOGICAL-DEVICE semaphore-create-info nil render-finished-semaphore-ptr)
                                VK13/VK_SUCCESS)
                          (not= (VK13/vkCreateFence LOGICAL-DEVICE fence-create-info nil in-flight-fence-ptr)
                                VK13/VK_SUCCESS))
                  (throw (str (RuntimeException. (str "Failed to create synchronization objects for frame: " i ".")))))
                (Frame. (.get image-available-semaphore-ptr 0)
                        (.get render-finished-semaphore-ptr 0)
                        (.get in-flight-fence-ptr 0)))
              (range MAX-FRAMES-IN-FLIGHT))))))

(def xx (volatile! 0))
(defn draw-frame []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [this-frame (frame/current-frame)
          image-index-ptr (.mallocInt stack 1)
          _ (do (VK13/vkWaitForFences LOGICAL-DEVICE (frame/get-in-flight-fence-ptr this-frame) true infinite-timeout)
                (KHRSwapchain/vkAcquireNextImageKHR LOGICAL-DEVICE SWAP-CHAIN-POINTER infinite-timeout
                                                    (frame/get-image-available-semaphore-ptr this-frame)
                                                    VK13/VK_NULL_HANDLE image-index-ptr))
          wait-semaphores (frame/alloc-image-available-semaphore-ptr this-frame stack)
          wait-stages (.ints stack VK13/VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
          signal-semaphores (frame/alloc-render-finished-semaphore-ptr this-frame stack)
          submit-info (doto (VkSubmitInfo/calloc stack)
                        (.sType VK13/VK_STRUCTURE_TYPE_SUBMIT_INFO)
                        (.waitSemaphoreCount 1)
                        (.pWaitSemaphores wait-semaphores)
                        (.pWaitDstStageMask wait-stages)
                        (.pCommandBuffers (.pointers stack ^Pointer (nth COMMAND-BUFFERS (.get image-index-ptr 0))))
                        (.pSignalSemaphores signal-semaphores))
          _ (VK13/vkResetFences LOGICAL-DEVICE (frame/alloc-in-flight-fence-ptr this-frame stack))
          _ (when (not= (VK13/vkQueueSubmit GRAPHICS-QUEUE submit-info (frame/get-in-flight-fence-ptr this-frame))
                        VK13/VK_SUCCESS)
              (throw (RuntimeException. "Failed to submit draw command buffer.")))
          present-info (doto (VkPresentInfoKHR/calloc stack)
                         (.sType KHRSwapchain/VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                         (.pWaitSemaphores signal-semaphores)
                         (.swapchainCount 1)
                         (.pSwapchains (.longs stack SWAP-CHAIN-POINTER))
                         (.pImageIndices image-index-ptr)
                         (.pResults nil))]
      (KHRSwapchain/vkQueuePresentKHR PRESENT-QUEUE present-info)
      (frame/next-frame))))
