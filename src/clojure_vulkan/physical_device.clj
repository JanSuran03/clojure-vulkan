(ns clojure-vulkan.physical-device
  (:require [clojure-vulkan.globals :refer [physical-device vulkan-instance]]
            [clojure-vulkan.util :as util]
            [clojure.pprint :as pp])
  (:import (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK13 VkPhysicalDevice VkPhysicalDeviceFeatures VkPhysicalDeviceProperties
                             VkQueueFamilyProperties)
           (org.lwjgl PointerBuffer)))

(defn- find-queue-families [^VkPhysicalDevice device]
  (util/with-memory-stack-push ^MemoryStack stack
    (let [queue-family-count-ptr (.ints stack 0)
          _ (VK13/vkGetPhysicalDeviceQueueFamilyProperties device queue-family-count-ptr nil)
          queue-family-count (.get queue-family-count-ptr 0)
          queue-families-ptr (VkQueueFamilyProperties/malloc queue-family-count stack)
          _ (VK13/vkGetPhysicalDeviceQueueFamilyProperties device queue-family-count-ptr queue-families-ptr)]
      (pp/pprint {:device             device
                  :queue-family-count queue-family-count
                  :queue-families     (for [i (range queue-family-count)]
                                        (.get queue-families-ptr ^long i))}))))

(defn- pick-suitable-device [^PointerBuffer devices-ptr device-count]
  (let [devices (object-array device-count)]
    (util/with-memory-stack-push ^MemoryStack stack
      (dotimes [i device-count]
        (let [device (VkPhysicalDevice. (.get devices-ptr ^long i) vulkan-instance)
              _ (find-queue-families device)
              ^VkPhysicalDeviceProperties device-properties (VkPhysicalDeviceProperties/calloc stack)
              ^VkPhysicalDeviceFeatures device-features (VkPhysicalDeviceFeatures/calloc stack)
              score (volatile! 0)]
          (do (VK13/vkGetPhysicalDeviceProperties device device-properties)
              (VK13/vkGetPhysicalDeviceFeatures device device-features)
              (when (= (.deviceType device-properties) VK13/VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU)
                (vswap! score + 1000))
              (when-not (.geometryShader device-features)
                (vreset! score 0))
              (aset devices i (list @score device)))))
      (sort-by first > devices)
      (aget devices 0))))

(defn pick-physical-device []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [device-count-ptr (.ints stack 0)
          _ (VK13/vkEnumeratePhysicalDevices vulkan-instance device-count-ptr nil)
          device-count (.get device-count-ptr 0)
          _ (when (zero? device-count)
              (throw (RuntimeException. "No GPU with Vulkan support found.")))
          physical-devices-ptr (.mallocPointer stack device-count)
          _ (VK13/vkEnumeratePhysicalDevices vulkan-instance device-count-ptr physical-devices-ptr)
          physical-device* (or (pick-suitable-device physical-devices-ptr device-count)
                               (throw (RuntimeException. "No suitable GPU found.")))]
      (alter-var-root #'physical-device (constantly physical-device*)))))