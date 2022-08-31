(ns clojure-vulkan.vulkan
  (:require [clojure-vulkan.command-buffers :as command-buffers]
            [clojure-vulkan.debug :as debug]
            [clojure-vulkan.frame :as frame]
            [clojure-vulkan.frame-buffers :as frame-buffers]
            [clojure-vulkan.globals :as globals]
            [clojure-vulkan.graphics-pipeline :as graphics-pipeline]
            [clojure-vulkan.image-views :as image-views]
            [clojure-vulkan.instance :as instance]
            [clojure-vulkan.logical-device-and-queue :as logical-device-and-queue]
            [clojure-vulkan.math.vertex :as vertex]
            [clojure-vulkan.physical-device :as physical-device]
            [clojure-vulkan.render :as render]
            [clojure-vulkan.render-pass :as render-pass]
            [clojure-vulkan.swap-chain :as swap-chain]
            [clojure-vulkan.texture :as texture]
            [clojure-vulkan.uniform :as uniform]
            [clojure-vulkan.util :as util]
            [clojure-vulkan.window-surface :as window-surface])
  (:import (clojure_vulkan.Vulkan VulkanGlobals)
           (org.lwjgl.glfw GLFW)
           (org.lwjgl.vulkan VK13)))

(defn init []
  (instance/create)
  (debug/setup-debug-messenger)
  (window-surface/create-surface)
  (physical-device/pick-physical-device)
  (logical-device-and-queue/create-logical-device)
  (swap-chain/create-swap-chain)
  (image-views/create-image-views)
  (render-pass/create-render-pass)
  (uniform/create-descriptor-set-layout)
  (graphics-pipeline/create-graphics-pipeline)
  (frame-buffers/create-frame-buffers)
  (command-buffers/create-command-pool)
  (texture/create-texture-image "pavian.jpg")
  (vertex/create-vertex-buffer)
  (vertex/create-index-buffer)
  (uniform/create-uniform-buffers)
  (uniform/create-descriptor-pool)
  (uniform/create-descriptor-sets)
  (command-buffers/create-command-buffers)
  (render/create-sync-objects)
  (reset! globals/old-time (GLFW/glfwGetTime)))

(defn terminate []
  (util/try-all
    #(util/log "Vulkan cleanup error occured: " (.getMessage ^Throwable %))
    (swap-chain/cleanup-swap-chain)
    (uniform/destroy-descriptor-pool)
    (.free VulkanGlobals/UNIFORM_BUFFERS)
    (uniform/destroy-descriptor-set-layout)
    (globals/set-global! globals/SWAP-CHAIN-POINTER VK13/VK_NULL_HANDLE)
    (.free globals/INDEX-BUFFER)
    (.free globals/VERTEX-BUFFER)
    (texture/destroy-texture)
    (texture/free-texture-memory)
    (frame/destroy-semaphores-and-fences)
    (command-buffers/destroy-command-pool)
    (graphics-pipeline/destroy-graphics-pipeline)
    (graphics-pipeline/destroy-pipeline-layout)
    (render-pass/destroy-render-pass)
    (window-surface/destroy-surface)
    (.free VulkanGlobals/LOGICAL_DEVICE)
    (.free VulkanGlobals/PHYSICAL_DEVICE)
    (when VulkanGlobals/VALIDATION_LAYERS_ENABLED
      (VulkanGlobals/disableValidationLayers)
      (debug/destroy-debug-messenger nil))
    (instance/destroy-instance)
    (.free VulkanGlobals/QUEUE_FAMILIES)
    (globals/reset-swap-chain-support-details)))

