(ns clojure-vulkan.command-buffers
  (:require [clojure-vulkan.globals :as globals :refer [COMMAND-BUFFERS COMMAND-POOL-POINTER DESCRIPTOR-SET-POINTERS
                                                        GRAPHICS-PIPELINE-POINTER INDEX-BUFFER
                                                        PIPELINE-LAYOUT-POINTER QUEUE-FAMILIES RENDER-PASS-POINTER
                                                        SWAP-CHAIN-EXTENT SWAP-CHAIN-FRAME-BUFFER-POINTERS VERTEX-BUFFER]]
            [clojure-vulkan.util :as util]
            [clojure-vulkan.math.vertex :as vertex])
  (:import (clojure_vulkan.Vulkan Buffer VulkanGlobals)
           (org.lwjgl.system MemoryStack)
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

      (if (= (VK13/vkCreateCommandPool (VulkanGlobals/getLogicalDevice) command-pool-create-info nil command-pool-ptr)
             VK13/VK_SUCCESS)
        (globals/set-global! COMMAND-POOL-POINTER (.get command-pool-ptr 0))
        (throw (RuntimeException. "Failed to create command pool."))))))

(defn destroy-command-buffers
  "No need for Vulkan cleanup, destroyed with their command pools."
  []
  (globals/reset-command-buffers))

(defn destroy-command-pool
  "Destroys command buffers as well."
  []
  (VK13/vkDestroyCommandPool (VulkanGlobals/getLogicalDevice) COMMAND-POOL-POINTER nil)
  (globals/reset-command-pool-ptr)
  (destroy-command-buffers))

(defn record-command-buffer [{:keys [^VkCommandBuffer command-buffer
                                     ^VkCommandBufferBeginInfo command-buffer-begin-info
                                     ^VkRenderPassBeginInfo render-pass-begin-info
                                     ^VkRect2D$Buffer scissor-buffers
                                     ^MemoryStack stack
                                     swap-chain-frame-buffer-pointer
                                     ^VkViewport$Buffer viewports-buffer
                                     command-buffer-index]}]
  (when (not= (VK13/vkBeginCommandBuffer command-buffer command-buffer-begin-info)
              VK13/VK_SUCCESS)
    (throw (RuntimeException. "Failed to begin recording command buffer.")))
  (.framebuffer render-pass-begin-info swap-chain-frame-buffer-pointer)
  (VK13/vkCmdBeginRenderPass command-buffer render-pass-begin-info VK13/VK_SUBPASS_CONTENTS_INLINE)
  (VK13/vkCmdBindPipeline command-buffer VK13/VK_PIPELINE_BIND_POINT_GRAPHICS GRAPHICS-PIPELINE-POINTER) ; graphics or compute pipeline?
  (VK13/vkCmdSetViewport command-buffer 0 viewports-buffer)
  (VK13/vkCmdSetScissor command-buffer 0 scissor-buffers)
  (let [vertex-buffers (.longs stack (.bufferPointer VERTEX-BUFFER))
        offsets (.longs stack 0)]
    (VK13/vkCmdBindVertexBuffers command-buffer 0 vertex-buffers offsets))
  (VK13/vkCmdBindIndexBuffer command-buffer (.bufferPointer ^Buffer INDEX-BUFFER) 0 VK13/VK_INDEX_TYPE_UINT16) ;; short
  (VK13/vkCmdBindDescriptorSets command-buffer
                                VK13/VK_PIPELINE_BIND_POINT_GRAPHICS
                                PIPELINE-LAYOUT-POINTER
                                0
                                (.longs stack ^long (nth DESCRIPTOR-SET-POINTERS command-buffer-index))
                                nil)
  (VK13/vkCmdDrawIndexed command-buffer (count vertex/indices)
                         #_instance-count 1
                         #_first-index 0
                         #_vertex-offset 0
                         #_first-instance 0)
  (VK13/vkCmdEndRenderPass command-buffer)
  (when (not= (VK13/vkEndCommandBuffer command-buffer)
              VK13/VK_SUCCESS)
    (throw (RuntimeException. "Failed to record command buffer."))))

(defn create-command-buffers []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [command-buffers-count 3
          command-buffer-allocate-info (doto (VkCommandBufferAllocateInfo/calloc stack)
                                         (.sType VK13/VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                                         (.commandPool COMMAND-POOL-POINTER)
                                         (.level VK13/VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                                         (.commandBufferCount command-buffers-count))
          command-buffers-ptr (.mallocPointer stack command-buffers-count)
          render-area (doto (VkRect2D/calloc stack)
                        (.offset (.set (VkOffset2D/calloc stack) 0 0))
                        (.extent SWAP-CHAIN-EXTENT))
          _ (if (= (VK13/vkAllocateCommandBuffers (VulkanGlobals/getLogicalDevice) command-buffer-allocate-info command-buffers-ptr)
                   VK13/VK_SUCCESS)
              (globals/set-global! COMMAND-BUFFERS (mapv #(VkCommandBuffer. (.get command-buffers-ptr ^int %) (VulkanGlobals/getLogicalDevice))
                                                         (range command-buffers-count)))
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
                             (.y (float (.height SWAP-CHAIN-EXTENT))) ; ESSENTIAL FOR Y-AXIS VIEWPORT FLIPPING
                             (.width (float (.width SWAP-CHAIN-EXTENT)))
                             (.height (float (- (.height SWAP-CHAIN-EXTENT)))) ; ESSENTIAL FOR Y-AXIS VIEWPORT FLIPPING
                             (.minDepth (float 0))
                             (.maxDepth (float 1)))
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
                                :swap-chain-frame-buffer-pointer (nth SWAP-CHAIN-FRAME-BUFFER-POINTERS i)
                                :viewports-buffer                viewports-buffer
                                :stack                           stack
                                :command-buffer-index            i})))))