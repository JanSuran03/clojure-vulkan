(ns clojure-vulkan.render
  (:require [clojure-vulkan.swap-chain :as swap-chain]
            [clojure-vulkan.util :as util]
            [clojure-vulkan.uniform :as uniform])
  (:import (clojure_vulkan.Vulkan VulkanGlobals Frame)
           (org.lwjgl.system MemoryStack Pointer)
           (org.lwjgl.vulkan KHRSwapchain VK13 VkFenceCreateInfo VkPresentInfoKHR VkSemaphoreCreateInfo VkSubmitInfo)))

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
          image-available-semaphore-ptr (.mallocLong stack Frame/MAX_FRAMES_IN_FLIGHT)
          render-finished-semaphore-ptr (.mallocLong stack Frame/MAX_FRAMES_IN_FLIGHT)
          in-flight-fence-ptr (.mallocLong stack Frame/MAX_FRAMES_IN_FLIGHT)]
      (Frame/createFrames
        (mapv (fn [^Integer i]
                (when (or (not= (VK13/vkCreateSemaphore (VulkanGlobals/getLogicalDevice) semaphore-create-info nil image-available-semaphore-ptr)
                                VK13/VK_SUCCESS)
                          (not= (VK13/vkCreateSemaphore (VulkanGlobals/getLogicalDevice) semaphore-create-info nil render-finished-semaphore-ptr)
                                VK13/VK_SUCCESS)
                          (not= (VK13/vkCreateFence (VulkanGlobals/getLogicalDevice) fence-create-info nil in-flight-fence-ptr)
                                VK13/VK_SUCCESS))
                  (throw (str (RuntimeException. (str "Failed to create synchronization objects for frame: " i ".")))))
                (doto (Frame.)
                  (.imageAvailableSemaphorePointer (.get image-available-semaphore-ptr 0))
                  (.renderFinishedSemaphorePointer (.get render-finished-semaphore-ptr 0))
                  (.inFlightFencePointer (.get in-flight-fence-ptr 0))))
              (range Frame/MAX_FRAMES_IN_FLIGHT))))))

(defn draw-frame []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [this-frame (Frame/currentFrame)
          image-index-ptr (.mallocInt stack 1)
          _ (VK13/vkWaitForFences (VulkanGlobals/getLogicalDevice) (.inFlightFencePointer this-frame) true infinite-timeout)
          acquire-result (KHRSwapchain/vkAcquireNextImageKHR (VulkanGlobals/getLogicalDevice) (.get VulkanGlobals/SWAP_CHAIN_POINTER) infinite-timeout
                                                             (.imageAvailableSemaphorePointer this-frame)
                                                             VK13/VK_NULL_HANDLE image-index-ptr)]
      (cond (= KHRSwapchain/VK_ERROR_OUT_OF_DATE_KHR acquire-result)
            (swap-chain/recreate-swap-chain)                ;; and return: last expression

            (#{VK13/VK_SUCCESS KHRSwapchain/VK_SUBOPTIMAL_KHR} acquire-result)
            (let [wait-semaphores (.longs stack (.imageAvailableSemaphorePointer this-frame))
                  wait-stages (.ints stack VK13/VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                  signal-semaphores (.longs stack (.renderFinishedSemaphorePointer this-frame))
                  _ (uniform/update-uniform-buffer (.get image-index-ptr 0) stack)
                  submit-info (doto (VkSubmitInfo/calloc stack)
                                (.sType VK13/VK_STRUCTURE_TYPE_SUBMIT_INFO)
                                (.waitSemaphoreCount 1)
                                (.pWaitSemaphores wait-semaphores)
                                (.pWaitDstStageMask wait-stages)
                                (.pCommandBuffers (.pointers stack ^Pointer (.get VulkanGlobals/COMMAND_BUFFERS (.get image-index-ptr 0))))
                                (.pSignalSemaphores signal-semaphores))
                  _ (VK13/vkResetFences (VulkanGlobals/getLogicalDevice) (.longs stack (.inFlightFencePointer this-frame)))
                  _ (when (not= (VK13/vkQueueSubmit (.get VulkanGlobals/GRAPHICS_QUEUE) submit-info (.inFlightFencePointer this-frame))
                                VK13/VK_SUCCESS)
                      (throw (RuntimeException. "Failed to submit draw command buffer.")))
                  present-info (doto (VkPresentInfoKHR/calloc stack)
                                 (.sType KHRSwapchain/VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                                 (.pWaitSemaphores signal-semaphores)
                                 (.swapchainCount 1)
                                 (.pSwapchains (.longs stack (.get VulkanGlobals/SWAP_CHAIN_POINTER)))
                                 (.pImageIndices image-index-ptr)
                                 (.pResults nil))
                  present-result (KHRSwapchain/vkQueuePresentKHR (.get VulkanGlobals/PRESENT_QUEUE) present-info)]
              (cond
                (or (#{KHRSwapchain/VK_ERROR_OUT_OF_DATE_KHR KHRSwapchain/VK_SUBOPTIMAL_KHR} present-result)
                    Frame/isFrameBufferResized)
                (do (Frame/setFrameBufferResized false)
                    (swap-chain/recreate-swap-chain))

                VK13/VK_SUCCESS
                :ok

                :else
                (throw (RuntimeException. "Failed to present swap chain image.")))
              (Frame/nextFrame))

            :else
            (throw (RuntimeException. "Failed to acquire swap chain image."))))))
