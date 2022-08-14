(ns clojure-vulkan.render-pass
  (:require [clojure-vulkan.globals :as globals :refer [LOGICAL-DEVICE RENDER-PASS-POINTER SWAP-CHAIN-IMAGE-FORMAT]]
            [clojure-vulkan.util :as util])
  (:import (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VkAttachmentDescription VK13 KHRSwapchain VkAttachmentReference VkSubpassDescription VkRenderPassCreateInfo)))

(defn create-render-pass []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [color-attachment-descriptions (doto (VkAttachmentDescription/calloc 1 stack)
                                          (.format SWAP-CHAIN-IMAGE-FORMAT)
                                          (.samples VK13/VK_SAMPLE_COUNT_1_BIT)
                                          (.loadOp VK13/VK_ATTACHMENT_LOAD_OP_CLEAR)
                                          (.storeOp VK13/VK_ATTACHMENT_STORE_OP_STORE)
                                          (.stencilLoadOp VK13/VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                                          (.stencilStoreOp VK13/VK_ATTACHMENT_STORE_OP_DONT_CARE)
                                          (.initialLayout VK13/VK_IMAGE_LAYOUT_UNDEFINED)
                                          (.finalLayout KHRSwapchain/VK_IMAGE_LAYOUT_PRESENT_SRC_KHR))
          color-attachment-references (doto (VkAttachmentReference/calloc 1 stack)
                                       (.attachment 0)
                                       (.layout VK13/VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL))
          subpass-descriptions (doto (VkSubpassDescription/calloc 1 stack)
                                 (.pipelineBindPoint VK13/VK_PIPELINE_BIND_POINT_GRAPHICS)
                                 (.colorAttachmentCount 1)
                                 (.pColorAttachments color-attachment-references))
          render-pass-create-info (doto (VkRenderPassCreateInfo/calloc stack)
                                    (.sType VK13/VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                                    (.pAttachments color-attachment-descriptions)
                                    (.pSubpasses subpass-descriptions))
          render-pass-ptr (.longs stack VK13/VK_NULL_HANDLE)]
      (when (not= (VK13/vkCreateRenderPass LOGICAL-DEVICE render-pass-create-info nil render-pass-ptr)
                  VK13/VK_SUCCESS)
        (throw (RuntimeException. "Failed to create render pass.")))
      (alter-var-root #'RENDER-PASS-POINTER (constantly (.get render-pass-ptr 0))))))

(defn destroy-render-pass []
  (VK13/vkDestroyRenderPass LOGICAL-DEVICE RENDER-PASS-POINTER nil)
  (globals/reset-render-pass-ptr))