(ns clojure-vulkan.command-buffers
  (:require [clojure-vulkan.globals :as globals :refer [COMMAND-BUFFERS COMMAND-POOL-POINTER GRAPHICS-PIPELINE-POINTER LOGICAL-DEVICE QUEUE-FAMILIES
                                                        RENDER-PASS-POINTER SWAP-CHAIN-EXTENT SWAP-CHAIN-FRAME-BUFFER-POINTERS-VECTOR]]
            [clojure-vulkan.util :as util])
  (:import (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK13 VkClearColorValue VkClearValue VkCommandBuffer
                             VkCommandBufferAllocateInfo VkCommandBufferBeginInfo VkCommandPoolCreateInfo VkOffset2D
                             VkRect2D VkRect2D$Buffer VkRenderPassBeginInfo VkViewport VkViewport$Buffer)))

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

(defn record-command-buffer [{:keys [^VkCommandBuffer command-buffer
                                     ^VkCommandBufferBeginInfo command-buffer-begin-info
                                     ^VkRenderPassBeginInfo render-pass-begin-info
                                     ^VkRect2D$Buffer scissor-buffers
                                     swap-chain-frame-buffer-pointer
                                     ^VkViewport$Buffer viewports-buffer]}]
  (when (not= (VK13/vkBeginCommandBuffer command-buffer command-buffer-begin-info)
              VK13/VK_SUCCESS)
    (throw (RuntimeException. "Failed to begin recording command buffer.")))
  (.framebuffer render-pass-begin-info swap-chain-frame-buffer-pointer)
  (VK13/vkCmdBeginRenderPass command-buffer render-pass-begin-info VK13/VK_SUBPASS_CONTENTS_INLINE)
  (VK13/vkCmdBindPipeline command-buffer VK13/VK_PIPELINE_BIND_POINT_GRAPHICS GRAPHICS-PIPELINE-POINTER) ; graphics or compute pipeline?
  (VK13/vkCmdSetViewport command-buffer 0 viewports-buffer)
  (VK13/vkCmdSetScissor command-buffer 0 scissor-buffers)
  (VK13/vkCmdDraw command-buffer 3 1 0 0)
  (VK13/vkCmdEndRenderPass command-buffer)
  (when (not= (VK13/vkEndCommandBuffer command-buffer)
              VK13/VK_SUCCESS)
    (throw (RuntimeException. "Failed to record command buffer."))))

(defn create-command-buffers []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [command-buffers-count #_(count SWAP-CHAIN-FRAME-BUFFER-POINTERS-VECTOR) 1
          command-buffer-allocate-info (doto (VkCommandBufferAllocateInfo/calloc stack)
                                         (.sType VK13/VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                                         (.commandPool COMMAND-POOL-POINTER)
                                         (.level VK13/VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                                         (.commandBufferCount 1))
          command-buffers-ptr (.mallocPointer stack command-buffers-count)
          render-area (doto (VkRect2D/calloc stack)
                        (.offset (.set (VkOffset2D/calloc stack) 0 0))
                        (.extent SWAP-CHAIN-EXTENT))
          _ (if (= (VK13/vkAllocateCommandBuffers LOGICAL-DEVICE command-buffer-allocate-info command-buffers-ptr)
                   VK13/VK_SUCCESS)
              (dotimes [i command-buffers-count]
                (alter-var-root #'COMMAND-BUFFERS conj (VkCommandBuffer. (.get command-buffers-ptr i) LOGICAL-DEVICE)))
              (throw (RuntimeException. "Failed to allocate command buffers.")))
          command-buffer-begin-info (doto (VkCommandBufferBeginInfo/calloc stack)
                                      (.sType VK13/VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                                      (.flags 0)
                                      (.pInheritanceInfo nil) ; only relevant for secondary command buffers
                                      )
          clear-values (VkClearValue/calloc 1 stack)
          _ (.float32 ^VkClearColorValue (.color clear-values) (.floats stack 0 0 0 1))
          viewports-buffer (doto (VkViewport/calloc 1 stack)
                             (.x (float 0))
                             (.y (float 0))
                             (.width (float (.width SWAP-CHAIN-EXTENT)))
                             (.height (float (.height SWAP-CHAIN-EXTENT)))
                             (.minDepth (float 0))
                             (.maxDepth (float 1)))
          _ (println "VIEWPORTS:" viewports-buffer)
          render-pass-begin-info (doto (VkRenderPassBeginInfo/calloc stack)
                                   (.sType VK13/VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                                   (.renderPass RENDER-PASS-POINTER)
                                   (.renderArea render-area)
                                   (.clearValueCount 1)
                                   (.pClearValues clear-values))
          scissor-buffers (doto (VkRect2D/calloc 1 stack)
                            (.offset (.set (VkOffset2D/calloc stack) 0 0))
                            (.extent SWAP-CHAIN-EXTENT))]
      (dotimes [i command-buffers-count]
        (record-command-buffer {:command-buffer                  (nth COMMAND-BUFFERS i)
                                :command-buffer-begin-info       command-buffer-begin-info
                                :render-pass-begin-info          render-pass-begin-info
                                :scissor-buffers                 scissor-buffers
                                :swap-chain-frame-buffer-pointer (nth SWAP-CHAIN-FRAME-BUFFER-POINTERS-VECTOR i)
                                :viewports-buffer                viewports-buffer})))))

(defn destroy-command-buffers
  "No need for Vulkan cleanup, destroyed with their command pools."
  []
  (globals/reset-command-buffers))