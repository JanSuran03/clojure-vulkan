(ns clojure-vulkan.globals
  (:require [clojure-vulkan.util :refer [nullptr]])
  (:import (org.lwjgl.vulkan VkInstance VkPhysicalDevice VK13 VkDevice VkQueue)))

(defn- reset-to-null [_] nil)
(defn- reset-to-vk-null [_] VK13/VK_NULL_HANDLE)

(def ^long debug-messenger-ptr VK13/VK_NULL_HANDLE)
(defn reset-debug-messenger [] (alter-var-root #'debug-messenger-ptr reset-to-vk-null))

(def ^VkInstance vulkan-instance nil)
(defn reset-vulkan-instance [] (alter-var-root #'vulkan-instance reset-to-null))

(def ^VkPhysicalDevice physical-device nil)
(defn reset-physical-device [] (alter-var-root #'physical-device reset-to-null))

(def ^VkDevice logical-device nil)
(defn reset-logical-device [] (alter-var-root #'logical-device reset-to-null))

(def queue-families {})
(defn reset-queue-families [] (alter-var-root #'queue-families empty))

(def ^VkQueue graphics-queue nil)
(defn reset-graphics-queue [] (alter-var-root #'graphics-queue reset-to-null))

(def ^long window-surface-ptr VK13/VK_NULL_HANDLE)
(defn reset-window-surface [] (alter-var-root #'window-surface-ptr reset-to-vk-null))

(def ^long window-ptr nullptr)
(defn reset-window-ptr [] (alter-var-root #'window-ptr reset-to-vk-null))