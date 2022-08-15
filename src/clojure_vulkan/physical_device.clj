(ns clojure-vulkan.physical-device
  (:require [clojure-vulkan.globals :as globals :refer [PHYSICAL-DEVICE QUEUE-FAMILIES VULKAN-INSTANCE WINDOW-SURFACE-POINTER]]
            [clojure-vulkan.swap-chain :as swap-chain]
            [clojure-vulkan.util :as util])
  (:import (java.nio IntBuffer)
           (org.lwjgl PointerBuffer)
           (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan KHRSurface KHRSwapchain VK13 VkExtensionProperties VkPhysicalDevice VkPhysicalDeviceFeatures
                             VkPhysicalDeviceProperties VkQueueFamilyProperties VkSurfaceFormatKHR$Buffer)))

(def device-extensions (list KHRSwapchain/VK_KHR_SWAPCHAIN_EXTENSION_NAME))

(defn- check-device-extension-support [^VkPhysicalDevice device]
  (util/with-memory-stack-push ^MemoryStack stack
    (let [extension-count (.ints stack 0)
          _ (VK13/vkEnumerateDeviceExtensionProperties device "" extension-count nil)
          available-extensions (VkExtensionProperties/malloc (.get extension-count 0) stack)
          _ (VK13/vkEnumerateDeviceExtensionProperties device "" extension-count available-extensions)
          available-extensions-set (->> available-extensions
                                        util/buffer->seq
                                        (map (memfn ^VkExtensionProperties extensionNameString))
                                        set)]
      (every? #(contains? available-extensions-set %) device-extensions))))

(defn- find-queue-families [^VkPhysicalDevice device]
  (util/with-memory-stack-push ^MemoryStack stack
    (let [queue-family-count-ptr (.ints stack 0)
          _ (VK13/vkGetPhysicalDeviceQueueFamilyProperties device queue-family-count-ptr nil)
          queue-family-count (.get queue-family-count-ptr 0)
          queue-families-ptr (VkQueueFamilyProperties/malloc queue-family-count stack)
          _ (VK13/vkGetPhysicalDeviceQueueFamilyProperties device queue-family-count-ptr queue-families-ptr)
          graphics-and-present-family-map
          (reduce (fn [family-map ^long i]
                    (if (and (:graphics-family family-map)
                             (:present-family family-map))
                      (reduced family-map)
                      (let [^VkQueueFamilyProperties queue-family (.get queue-families-ptr i)
                            queue-flags (.queueFlags queue-family)
                            present-support (.ints stack VK13/VK_FALSE)]
                        (KHRSurface/vkGetPhysicalDeviceSurfaceSupportKHR device i WINDOW-SURFACE-POINTER present-support)
                        (cond-> family-map
                                (bit-and queue-flags VK13/VK_QUEUE_GRAPHICS_BIT) (assoc :graphics-family i)
                                (= (.get present-support 0) VK13/VK_TRUE) (assoc :present-family i)))))
                  {}
                  (range queue-family-count))]
      graphics-and-present-family-map)))

(defn- pick-suitable-device [^PointerBuffer devices-ptr device-count]
  (let [devices (volatile! ())
        graphics-family* (volatile! nil)
        present-family* (volatile! nil)]
    (util/with-memory-stack-push ^MemoryStack stack
      (dotimes [i device-count]
        (let [device (VkPhysicalDevice. (.get devices-ptr ^long i) VULKAN-INSTANCE)
              {:keys [graphics-family present-family]} (find-queue-families device)
              ^VkPhysicalDeviceProperties device-properties (VkPhysicalDeviceProperties/calloc stack)
              ^VkPhysicalDeviceFeatures device-features (VkPhysicalDeviceFeatures/calloc stack)
              score (volatile! 0)
              query-swap-chain-support (fn []
                                         (let [{:keys [formats-ptr present-modes-ptr surface-capabilities present-modes-count]}
                                               (swap-chain/query-swap-chain-support device)]
                                           (and formats-ptr
                                                (.hasRemaining ^VkSurfaceFormatKHR$Buffer formats-ptr)
                                                present-modes-ptr
                                                (.hasRemaining ^IntBuffer present-modes-ptr))))]
          (if (and graphics-family present-family
                   (check-device-extension-support device)
                   (query-swap-chain-support))
            (do (VK13/vkGetPhysicalDeviceProperties device device-properties)
                (VK13/vkGetPhysicalDeviceFeatures device device-features)
                (when (= (.deviceType device-properties) VK13/VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU)
                  (vswap! score + 1000))
                (when-not (.geometryShader device-features)
                  (vreset! score 0))
                (vswap! devices conj (list @score device))
                (vreset! graphics-family* graphics-family)
                (vreset! present-family* present-family))
            (vreset! score nil))))
      {:graphics-family* @graphics-family*
       :present-family*  @present-family*
       :physical-device* (->> @devices (remove (fn [[score _]]
                                                 (nil? score)))
                              (sort-by (fn [[score _]]
                                         score))
                              first
                              second)})))

(defn pick-physical-device []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [device-count-ptr (.ints stack 0)
          _ (VK13/vkEnumeratePhysicalDevices VULKAN-INSTANCE device-count-ptr nil)
          device-count (.get device-count-ptr 0)
          _ (when (zero? device-count)
              (throw (RuntimeException. "No GPU with Vulkan support found.")))
          physical-devices-ptr (.mallocPointer stack device-count)
          _ (VK13/vkEnumeratePhysicalDevices VULKAN-INSTANCE device-count-ptr physical-devices-ptr)
          {:keys [graphics-family* present-family* physical-device*]} (pick-suitable-device physical-devices-ptr device-count)]
      (when-not physical-device*
        (throw (RuntimeException. "No suitable GPU found.")))
      (globals/set-global! PHYSICAL-DEVICE physical-device*)
      (alter-var-root #'QUEUE-FAMILIES conj {:graphics-family graphics-family*
                                             :present-family  present-family*}))))
