(ns clojure-vulkan.globals
  (:import (clojure_vulkan.Vulkan Buffer)
           (org.lwjgl.vulkan VK13  VkInstance VkQueue)))

(defmacro set-global! [global-var new-value]
  `(alter-var-root (var ~global-var) (constantly ~new-value)))

(defn- reset-to-nil [_] nil)
(defn- reset-to-vk-null [_] VK13/VK_NULL_HANDLE)

(def ^:dynamic *config* {})
;; **************************************************
;; GLOBALS
;; **************************************************
(def ^Long DEBUG-MESSENGER-POINTER VK13/VK_NULL_HANDLE)
(defn reset-debug-messenger [] (alter-var-root #'DEBUG-MESSENGER-POINTER reset-to-vk-null))

(def ^VkInstance VULKAN-INSTANCE nil)
(defn reset-vulkan-instance [] (alter-var-root #'VULKAN-INSTANCE reset-to-nil))

(def ^VkQueue GRAPHICS-QUEUE nil)
(defn reset-graphics-queue [] (alter-var-root #'GRAPHICS-QUEUE reset-to-nil))

(def ^VkQueue PRESENT-QUEUE nil)
(defn reset-present-queue [] (alter-var-root #'PRESENT-QUEUE reset-to-nil))

(def ^Long WINDOW-SURFACE-POINTER VK13/VK_NULL_HANDLE)
(defn reset-window-surface [] (alter-var-root #'WINDOW-SURFACE-POINTER reset-to-vk-null))

(def ^Long WINDOW-POINTER VK13/VK_NULL_HANDLE)
(defn reset-window-ptr [] (alter-var-root #'WINDOW-POINTER reset-to-vk-null))
;; **************************************************
;; SWAP CHAIN
;; **************************************************
(def SWAP-CHAIN-SUPPORT-DETAILS {})
(defn reset-swap-chain-support-details [] (alter-var-root #'SWAP-CHAIN-SUPPORT-DETAILS empty))

(def ^Long SWAP-CHAIN-POINTER VK13/VK_NULL_HANDLE)
(defn reset-swap-chain-ptr [] (alter-var-root #'SWAP-CHAIN-POINTER reset-to-vk-null))

(def SWAP-CHAIN-IMAGES [])
(defn reset-swap-chain-images [] (alter-var-root #'SWAP-CHAIN-IMAGES empty))

(def ^Integer SWAP-CHAIN-IMAGE-FORMAT VK13/VK_NULL_HANDLE)
(defn reset-swap-chain-image-format [] (alter-var-root #'SWAP-CHAIN-IMAGE-FORMAT reset-to-vk-null))

(def SWAP-CHAIN-IMAGE-VIEWS-POINTERS [])
(defn reset-swap-chain-image-views [] (alter-var-root #'SWAP-CHAIN-IMAGE-VIEWS-POINTERS empty))

(def ^Long PIPELINE-LAYOUT-POINTER VK13/VK_NULL_HANDLE)
(defn reset-pipeline-layout-ptr [] (alter-var-root #'PIPELINE-LAYOUT-POINTER reset-to-vk-null))

(def ^Long RENDER-PASS-POINTER VK13/VK_NULL_HANDLE)
(defn reset-render-pass-ptr [] (alter-var-root #'RENDER-PASS-POINTER reset-to-vk-null))

(def ^Long GRAPHICS-PIPELINE-POINTER VK13/VK_NULL_HANDLE)
(defn reset-graphics-pipeline-ptr [] (alter-var-root #'GRAPHICS-PIPELINE-POINTER reset-to-vk-null))

(def SWAP-CHAIN-FRAME-BUFFER-POINTERS [])
(defn reset-swap-chain-frame-buffers [] (alter-var-root #'SWAP-CHAIN-FRAME-BUFFER-POINTERS empty))

(def ^Long COMMAND-POOL-POINTER VK13/VK_NULL_HANDLE)
(defn reset-command-pool-ptr [] (alter-var-root #'COMMAND-POOL-POINTER reset-to-vk-null))
;; **************************************************
;; BUFFERS
;; **************************************************

(def ^Buffer VERTEX-BUFFER nil)

(def ^Buffer INDEX-BUFFER nil)
;; **************************************************
;; UNIFORMS
;; **************************************************
(def DESCRIPTOR-POOL-POINTER VK13/VK_NULL_HANDLE)
(defn reset-descriptor-pool-ptr [] (alter-var-root #'DESCRIPTOR-POOL-POINTER reset-to-vk-null))

(def ^Long DESCRIPTOR-SET-LAYOUT-POINTER VK13/VK_NULL_HANDLE)
(defn reset-descriptor-set-layout-ptr [] (alter-var-root #'DESCRIPTOR-SET-LAYOUT-POINTER reset-to-vk-null))

(def DESCRIPTOR-SET-POINTERS [])
(defn reset-descriptor-sets-ptrs [] (alter-var-root #'DESCRIPTOR-SET-POINTERS empty))

(def old-time (atom 0))
(def delta-time (atom 0))

(def IMAGE-POINTER VK13/VK_NULL_HANDLE)
(defn reset-image-ptr [] (alter-var-root #'IMAGE-POINTER reset-to-vk-null))

(def IMAGE-MEMORY-POINTER VK13/VK_NULL_HANDLE)
(defn reset-image-memory-ptr [] (alter-var-root #'IMAGE-MEMORY-POINTER reset-to-vk-null))