(ns clojure-vulkan.globals
  (:require [clojure-vulkan.util :refer [nullptr]])
  (:import (org.lwjgl.vulkan VK13 VkDevice VkExtent2D VkInstance VkPhysicalDevice VkQueue)))

(defmacro set-global! [global-var new-value]
  `(alter-var-root (var ~global-var) (constantly ~new-value)))

(defn- reset-to-nil [_] nil)
(defn- reset-to-vk-null [_] VK13/VK_NULL_HANDLE)

(def ^Long DEBUG-MESSENGER-POINTER VK13/VK_NULL_HANDLE)
(defn reset-debug-messenger [] (alter-var-root #'DEBUG-MESSENGER-POINTER reset-to-vk-null))

(def ^VkInstance VULKAN-INSTANCE nil)
(defn reset-vulkan-instance [] (alter-var-root #'VULKAN-INSTANCE reset-to-nil))

(def ^VkPhysicalDevice PHYSICAL-DEVICE nil)
(defn reset-physical-device [] (alter-var-root #'PHYSICAL-DEVICE reset-to-nil))

(def ^VkDevice LOGICAL-DEVICE nil)
(defn reset-logical-device [] (alter-var-root #'LOGICAL-DEVICE reset-to-nil))

(def QUEUE-FAMILIES {})
(defn reset-queue-families [] (alter-var-root #'QUEUE-FAMILIES empty))

(def ^VkQueue GRAPHICS-QUEUE nil)
(defn reset-graphics-queue [] (alter-var-root #'GRAPHICS-QUEUE reset-to-nil))

(def ^VkQueue PRESENT-QUEUE nil)
(defn reset-present-queue [] (alter-var-root #'PRESENT-QUEUE reset-to-nil))

(def ^Long WINDOW-SURFACE-POINTER VK13/VK_NULL_HANDLE)
(defn reset-window-surface [] (alter-var-root #'WINDOW-SURFACE-POINTER reset-to-vk-null))

(def ^Long WINDOW-POINTER nullptr)
(defn reset-window-ptr [] (alter-var-root #'WINDOW-POINTER reset-to-vk-null))

(def SWAP-CHAIN-SUPPORT-DETAILS {})
(defn reset-swap-chain-support-details [] (alter-var-root #'SWAP-CHAIN-SUPPORT-DETAILS empty))

(def ^Long SWAP-CHAIN-POINTER VK13/VK_NULL_HANDLE)
(defn reset-swap-chain-ptr [] (alter-var-root #'SWAP-CHAIN-POINTER reset-to-vk-null))

(def SWAP-CHAIN-IMAGES [])
(defn reset-swap-chain-images [] (alter-var-root #'SWAP-CHAIN-IMAGES empty))

(def ^Integer SWAP-CHAIN-IMAGE-FORMAT VK13/VK_NULL_HANDLE)
(defn reset-swap-chain-image-format [] (alter-var-root #'SWAP-CHAIN-POINTER reset-to-vk-null))

(def ^VkExtent2D SWAP-CHAIN-EXTENT nil)
(defn reset-swap-chain-extent [] (alter-var-root #'SWAP-CHAIN-EXTENT reset-to-nil))

(def SWAP-CHAIN-IMAGE-VIEWS [])
(defn reset-swap-chain-image-views [] (alter-var-root #'SWAP-CHAIN-IMAGE-VIEWS empty))

(def ^Long PIPELINE-LAYOUT-POINTER nullptr)
(defn reset-pipeline-layout-ptr [] (alter-var-root #'PIPELINE-LAYOUT-POINTER reset-to-vk-null))

(def ^Long RENDER-PASS-POINTER nullptr)
(defn reset-render-pass-ptr [] (alter-var-root #'RENDER-PASS-POINTER reset-to-vk-null))

(def ^Long GRAPHICS-PIPELINE-POINTER nullptr)
(defn reset-graphics-pipeline-ptr [] (alter-var-root #'GRAPHICS-PIPELINE-POINTER reset-to-vk-null))

(def SWAP-CHAIN-FRAME-BUFFER-POINTERS-VECTOR [])
(defn reset-swap-chain-frame-buffers [] (alter-var-root #'SWAP-CHAIN-FRAME-BUFFER-POINTERS-VECTOR empty))

(def COMMAND-POOL-POINTER nullptr)
(defn reset-command-pool-ptr [] (alter-var-root #'COMMAND-POOL-POINTER reset-to-vk-null))