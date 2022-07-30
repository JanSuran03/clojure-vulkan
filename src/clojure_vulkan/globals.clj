(ns clojure-vulkan.globals
  (:require [clojure-vulkan.util :refer [nullptr]])
  (:import (org.lwjgl.vulkan VkInstance VkPhysicalDevice VK13 VkDevice VkQueue VkExtent2D)))

(defn- reset-to-nil [_] nil)
(defn- reset-to-vk-null [_] VK13/VK_NULL_HANDLE)

(def ^Long debug-messenger-ptr VK13/VK_NULL_HANDLE)
(defn reset-debug-messenger [] (alter-var-root #'debug-messenger-ptr reset-to-vk-null))

(def ^VkInstance vulkan-instance nil)
(defn reset-vulkan-instance [] (alter-var-root #'vulkan-instance reset-to-nil))

(def ^VkPhysicalDevice physical-device nil)
(defn reset-physical-device [] (alter-var-root #'physical-device reset-to-nil))

(def ^VkDevice logical-device nil)
(defn reset-logical-device [] (alter-var-root #'logical-device reset-to-nil))

(def queue-families {})
(defn reset-queue-families [] (alter-var-root #'queue-families empty))

(def ^VkQueue graphics-queue nil)
(defn reset-graphics-queue [] (alter-var-root #'graphics-queue reset-to-nil))

(def ^VkQueue present-queue nil)
(defn reset-present-queue [] (alter-var-root #'present-queue reset-to-nil))

(def ^Long window-surface-ptr VK13/VK_NULL_HANDLE)
(defn reset-window-surface [] (alter-var-root #'window-surface-ptr reset-to-vk-null))

(def ^Long window-ptr nullptr)
(defn reset-window-ptr [] (alter-var-root #'window-ptr reset-to-vk-null))

(def swap-chain-support-details {})
(defn reset-swap-chain-support-details [] (alter-var-root #'swap-chain-support-details empty))

(def ^Long swap-chain-ptr VK13/VK_NULL_HANDLE)
(defn reset-swap-chain-ptr [] (alter-var-root #'swap-chain-ptr reset-to-vk-null))

(def swap-chain-images [])
(defn reset-swap-chain-images [] (alter-var-root #'swap-chain-images empty))

(def ^Integer swap-chain-image-format VK13/VK_NULL_HANDLE)
(defn reset-swap-chain-image-format [] (alter-var-root #'swap-chain-ptr reset-to-vk-null))

(def ^VkExtent2D swap-chain-extent nil)
(defn reset-swap-chain-extent [] (alter-var-root #'swap-chain-extent reset-to-nil))