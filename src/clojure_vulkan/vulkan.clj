(ns clojure-vulkan.vulkan
  (:require [clojure-vulkan.command-buffers :as command-buffers]
            [clojure-vulkan.debug :as debug]
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
  (:import (clojure_vulkan.Vulkan VulkanGlobals Frame)
           (org.lwjgl.glfw GLFW)))

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
  (texture/create-texture-image-view)
  (texture/create-texture-sampler)
  (vertex/create-vertex-buffer)
  (vertex/create-index-buffer)
  (uniform/create-uniform-buffers)
  (uniform/create-descriptor-pool)
  (uniform/create-descriptor-sets)
  (command-buffers/create-command-buffers)
  (render/create-sync-objects)
  (set! VulkanGlobals/oldTime (GLFW/glfwGetTime)))

(defn terminate []
  (util/try-all
    #(util/log "Vulkan cleanup error occured: " (.getMessage ^Throwable %)
               \newline
               (.printStackTrace ^Throwable %))
    (swap-chain/cleanup-swap-chain)
    (.free VulkanGlobals/DESCRIPTOR_POOL_POINTER)
    (.free VulkanGlobals/UNIFORM_BUFFERS)
    (.free VulkanGlobals/DESCRIPTOR_SET_LAYOUT_POINTER)
    (.free globals/INDEX-BUFFER)
    (.free globals/VERTEX-BUFFER)
    (.free globals/TEXTURE)
    (Frame/cleanup)
    (.free VulkanGlobals/COMMAND_POOL)
    (graphics-pipeline/destroy-graphics-pipeline)
    (.free VulkanGlobals/PIPELINE_LAYOUT_POINTER)
    (.free VulkanGlobals/RENDER_PASS_POINTER)
    (.free VulkanGlobals/WINDOW_SURFACE_POINTER)
    (.free VulkanGlobals/LOGICAL_DEVICE)
    (.free VulkanGlobals/PHYSICAL_DEVICE)
    (when VulkanGlobals/VALIDATION_LAYERS_ENABLED
      (VulkanGlobals/disableValidationLayers)
      (.free VulkanGlobals/DEBUG_MESSENGER_POINTER))
    (.free VulkanGlobals/VULKAN_INSTANCE)
    (.free VulkanGlobals/QUEUE_FAMILIES)))

