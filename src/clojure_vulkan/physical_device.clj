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
          _ (VK13/vkGetPhysicalDeviceQueueFamilyProperties device queue-family-count-ptr queue-families-ptr)
          graphics-family (some (fn [^long i]
                                  (let [^VkQueueFamilyProperties queue-family (.get queue-families-ptr i)
                                        queue-flags (.queueFlags queue-family)]
                                    (when (bit-and queue-flags VK13/VK_QUEUE_GRAPHICS_BIT)
                                      queue-flags)))
                                (range queue-family-count))]
      graphics-family)))

(defn- pick-suitable-device [^PointerBuffer devices-ptr device-count]
  (let [devices (volatile! ())
        _ (util/with-memory-stack-push ^MemoryStack stack
            (dotimes [i device-count]
              (let [device (VkPhysicalDevice. (.get devices-ptr ^long i) vulkan-instance)
                    queue-families (find-queue-families device)
                    ^VkPhysicalDeviceProperties device-properties (VkPhysicalDeviceProperties/calloc stack)
                    ^VkPhysicalDeviceFeatures device-features (VkPhysicalDeviceFeatures/calloc stack)
                    score (volatile! 0)
                    _ (if queue-families
                        (do (VK13/vkGetPhysicalDeviceProperties device device-properties)
                            (VK13/vkGetPhysicalDeviceFeatures device device-features)
                            (when (= (.deviceType device-properties) VK13/VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU)
                              (vswap! score + 1000))
                            (when-not (.geometryShader device-features)
                              (vreset! score 0))
                            (vswap! devices conj (list @score device)))
                        (vreset! score nil))])))]
    (->> @devices (remove (fn [[score _]]
                            (nil? score)))
         (sort-by (fn [[score _]]
                    score))
         first
         second)))

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