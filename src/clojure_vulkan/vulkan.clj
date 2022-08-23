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
            [clojure-vulkan.uniform :as uniform]
            [clojure-vulkan.util :as util]
            [clojure-vulkan.validation-layers :as validation-layers]
            [clojure-vulkan.window-surface :as window-surface])
  (:import (org.lwjgl.vulkan VK13)))

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
  (vertex/create-vertex-buffer)
  (vertex/create-index-buffer)
  (uniform/create-uniform-buffers)
  (command-buffers/create-command-buffers)
  (render/create-sync-objects))

(defn find-resets []
  (->> (find-ns 'clojure-vulkan.globals)
       ns-publics
       keys
       (filter (fn [v]
                 (clojure.string/starts-with? v "reset-")))
       (map (fn [sym]
              (symbol "globals" (name sym))))))
; Vulkan instance
; debug messenger
; window surface
; physical device
; logical device
; swap chain
; image views
; render pass
; graphics pipeline
; frame buffers
; command pool
; command buffers
; sync objects

(defn terminate []
  (util/try-all
    #(util/log "Vulkan cleanup error occured: " (.getMessage ^Throwable %))
    (swap-chain/cleanup-swap-chain)
    (uniform/destroy-uniform-buffers)
    (uniform/destroy-descriptor-set-layout)
    (globals/set-global! globals/SWAP-CHAIN-POINTER VK13/VK_NULL_HANDLE)
    (vertex/destroy-index-buffer)
    (vertex/free-index-buffer-memory)
    (vertex/destroy-vertex-buffer)
    (vertex/free-vertex-buffer-memory)
    (frame/destroy-semaphores-and-fences)
    (command-buffers/destroy-command-pool)
    (graphics-pipeline/destroy-graphics-pipeline)
    (graphics-pipeline/destroy-pipeline-layout)
    (render-pass/destroy-render-pass)
    (window-surface/destroy-surface)
    (logical-device-and-queue/destroy-logical-device)
    (when validation-layers/*enable-validation-layers*
      (debug/destroy-debug-messenger nil))
    (instance/destroy-instance)
    (globals/reset-queue-families)
    (globals/reset-swap-chain-support-details)))

