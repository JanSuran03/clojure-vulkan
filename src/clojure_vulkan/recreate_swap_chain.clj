(ns clojure-vulkan.recreate-swap-chain
  (:require [clojure-vulkan.globals :refer [LOGICAL-DEVICE]]
            [clojure-vulkan.image-views :as image-views]
            [clojure-vulkan.swap-chain :as swap-chain]
            [clojure-vulkan.frame-buffers :as frame-buffers])
  (:import (org.lwjgl.vulkan VK13)))

(defn cleanup-swap-chain [])

(defn recreate-swap-chain []
  (VK13/vkDeviceWaitIdle LOGICAL-DEVICE)
  (cleanup-swap-chain)
  (swap-chain/create-swap-chain)
  (image-views/create-image-views)
  (frame-buffers/create-frame-buffers))