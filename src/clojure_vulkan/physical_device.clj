(ns clojure-vulkan.physical-device
  (:require [clojure-vulkan.globals :refer [physical-device vulkan-instance]]
            [clojure-vulkan.util :as util])
  (:import (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK13 VkPhysicalDevice)))

(defn- is-device-suitable? [^VkPhysicalDevice device]
  )

(defn pick-physical-device []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [device-count-ptr (.ints stack 0)
          _ (VK13/vkEnumeratePhysicalDevices vulkan-instance device-count-ptr nil)
          device-count (.get device-count-ptr 0)
          _ (when (zero? device-count)
              (throw (RuntimeException. "No GPU with Vulkan support found.")))
          physical-devices-ptr (.mallocPointer stack device-count)
          _ (VK13/vkEnumeratePhysicalDevices vulkan-instance device-count-ptr physical-devices-ptr)
          physical-device* (or (some (fn [^long i]
                                      (let [device (VkPhysicalDevice. (.get physical-devices-ptr i) vulkan-instance)]
                                        (when (is-device-suitable? device)
                                          device)))
                                    (range device-count))
                              (throw (RuntimeException. "No suitable GPU found.")))]
      (alter-var-root physical-device (constantly physical-device*)))))