(ns clojure-vulkan.render
  (:require [clojure-vulkan.globals :as globals :refer [COMMAND-BUFFERS GRAPHICS-PIPELINE-POINTER GRAPHICS-QUEUE LOGICAL-DEVICE RENDER-PASS-POINTER
                                                        SWAP-CHAIN-EXTENT SWAP-CHAIN-FRAME-BUFFER-POINTERS-VECTOR SWAP-CHAIN-POINTER]]
            [clojure-vulkan.util :as util])
  (:import (org.lwjgl.system MemoryStack Pointer)
           (org.lwjgl.vulkan KHRSwapchain VK13 VkClearColorValue VkClearValue VkCommandBuffer VkCommandBufferBeginInfo VkFenceCreateInfo VkOffset2D VkRect2D VkRenderPassBeginInfo VkSemaphoreCreateInfo VkSubmitInfo VkViewport)
           (clojure_vulkan Semaphores)))

(def ^Long infinite-timeout 0x7fffffffffffffff)

(def ^Semaphores SEMAPHORES (Semaphores. 0 0 0))

(defn create-sync-objects []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [semaphore-create-info (doto (VkSemaphoreCreateInfo/calloc stack)
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
      (.setImageAvailableSemaphorePointer SEMAPHORES (.get image-available-semaphore-ptr 0))
      (.setRenderFinishedSemaphorePointer SEMAPHORES (.get render-finished-semaphore-ptr 0))
      (.setInFlightFencePointer SEMAPHORES (.get in-flight-fence-ptr 0)))))

(defn destroy-semaphores-and-fences []
  (VK13/vkDestroySemaphore LOGICAL-DEVICE ^Long (.getImageAvailableSemaphorePointer SEMAPHORES) nil)
  (VK13/vkDestroySemaphore LOGICAL-DEVICE ^Long (.getRenderFinishedSemaphorePointer SEMAPHORES) nil)
  (VK13/vkDestroyFence LOGICAL-DEVICE ^Long (.getInFlightFencePointer SEMAPHORES) nil)
  (.setImageAvailableSemaphorePointer SEMAPHORES 0)
  (.setRenderFinishedSemaphorePointer SEMAPHORES 0)
  (.setInFlightFencePointer SEMAPHORES 0))

(defn draw-frame []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [image-index (.mallocInt stack 1)]
      (VK13/vkWaitForFences LOGICAL-DEVICE (.getInFlightFencePointer SEMAPHORES) true infinite-timeout)
      (VK13/vkResetFences LOGICAL-DEVICE (.getInFlightFencePointer SEMAPHORES))
      (KHRSwapchain/vkAcquireNextImageKHR LOGICAL-DEVICE SWAP-CHAIN-POINTER infinite-timeout
                                          ^Long (.getImageAvailableSemaphorePointer SEMAPHORES)
                                          VK13/VK_NULL_HANDLE image-index)
      (VK13/vkResetCommandBuffer (first COMMAND-BUFFERS) 0)
      ;; record command buffer
      (let [command-buffer-begin-info (doto (VkCommandBufferBeginInfo/calloc stack)
                                        (.sType VK13/VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO))
            _ (when (not= (VK13/vkBeginCommandBuffer ^VkCommandBuffer (first COMMAND-BUFFERS) command-buffer-begin-info)
                          VK13/VK_SUCCESS)
                (throw (RuntimeException. "Failed to begin recording command buffer.")))
            render-area (doto (VkRect2D/calloc stack)
                          (.offset (.set (VkOffset2D/calloc stack) 0 0))
                          (.extent SWAP-CHAIN-EXTENT))
            clear-values (VkClearValue/calloc 1 stack)
            _ (.float32 ^VkClearColorValue (.color clear-values) (.floats stack 0 1 0 1))
            render-pass-begin-info (doto (VkRenderPassBeginInfo/calloc stack)
                                     (.sType VK13/VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                                     (.renderPass RENDER-PASS-POINTER)
                                     (.framebuffer (first SWAP-CHAIN-FRAME-BUFFER-POINTERS-VECTOR))
                                     (.renderArea render-area)
                                     (.clearValueCount 1)
                                     (.pClearValues clear-values))
            _ (do (VK13/vkCmdBeginRenderPass (first COMMAND-BUFFERS) render-pass-begin-info VK13/VK_SUBPASS_CONTENTS_INLINE)
                  (VK13/vkCmdBindPipeline (first COMMAND-BUFFERS) VK13/VK_PIPELINE_BIND_POINT_GRAPHICS GRAPHICS-PIPELINE-POINTER))
            viewports-buffer (doto (VkViewport/calloc 1 stack)
                               (.x (float 0))
                               (.y (float 0))
                               (.width (float (.width SWAP-CHAIN-EXTENT)))
                               (.height (float (.height SWAP-CHAIN-EXTENT)))
                               (.minDepth (float 0))
                               (.maxDepth (float 1)))
            scissor-buffers (doto (VkRect2D/calloc 1 stack)
                              (.offset (.set (VkOffset2D/calloc stack) 0 0))
                              (.extent SWAP-CHAIN-EXTENT))]
        (VK13/vkCmdSetScissor (first COMMAND-BUFFERS) 0 scissor-buffers)
        (VK13/vkCmdSetViewport (first COMMAND-BUFFERS) 0 viewports-buffer)
        (VK13/vkCmdDraw (first COMMAND-BUFFERS) 3 1 0 0)
        (VK13/vkCmdEndRenderPass (first COMMAND-BUFFERS))
        (when (not= (VK13/vkEndCommandBuffer (first COMMAND-BUFFERS))
                    VK13/VK_SUCCESS)
          (throw (RuntimeException. "Failed to record command buffer."))))
      (let [wait-semaphores (.longs stack (.getImageAvailableSemaphorePointer SEMAPHORES))
            wait-stages (.ints stack VK13/VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
            signal-semaphores (.longs stack (.getRenderFinishedSemaphorePointer SEMAPHORES))
            submit-info (doto (VkSubmitInfo/calloc stack)
                          (.sType VK13/VK_STRUCTURE_TYPE_SUBMIT_INFO)
                          (.waitSemaphoreCount 1)
                          (.pWaitSemaphores wait-semaphores)
                          (.pWaitDstStageMask wait-stages)
                          (.pCommandBuffers (.pointers stack ^Pointer (first COMMAND-BUFFERS)))
                          (.pSignalSemaphores signal-semaphores))]
        (println "Got there")
        (when (not= (VK13/vkQueueSubmit GRAPHICS-QUEUE submit-info (.getInFlightFencePointer SEMAPHORES))
                    VK13/VK_SUCCESS)
          (throw (RuntimeException. "Failed to submit draw command buffer.")))
        (println "And got there as well.")))))
