(ns clojure-vulkan.frame
  (:require [clojure-vulkan.globals :as globals])
  (:import (clojure_vulkan.Vulkan VulkanGlobals)
           (java.nio LongBuffer)
           (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK13)
           (org.lwjgl.glfw GLFW)))

(def ^Integer MAX-FRAMES-IN-FLIGHT 2)
(def FRAMES [])
(def FRAME-BUFFER-RESIZED? (atom false))

(defrecord Frame [image-available-semaphore-ptr
                  render-finished-semaphore-ptr
                  in-flight-fence-ptr])

(defn ^Long get-image-available-semaphore-ptr [^Frame f]
  (.-image_available_semaphore_ptr f))

(defn ^LongBuffer alloc-image-available-semaphore-ptr [^Frame f ^MemoryStack stack]
  (.longs stack (get-image-available-semaphore-ptr f)))

(defn ^Long get-render-finished-semaphore-ptr [^Frame f]
  (.-render_finished_semaphore_ptr f))

(defn ^LongBuffer alloc-render-finished-semaphore-ptr [^Frame f ^MemoryStack stack]
  (.longs stack (get-render-finished-semaphore-ptr f)))

(defn ^Long get-in-flight-fence-ptr [^Frame f]
  (.-in_flight_fence_ptr f))

(defn ^LongBuffer alloc-in-flight-fence-ptr [^Frame f ^MemoryStack stack]
  (.longs stack (get-in-flight-fence-ptr f)))

(def ^Long ^:private current-frame-counter* (atom 0))

(defn get-current-frame-counter []
  @current-frame-counter*)

(defn current-frame []
  (nth FRAMES (get-current-frame-counter)))

(defn next-frame []
  (let [new-time (GLFW/glfwGetTime)]
    (reset! globals/delta-time (- new-time @globals/old-time))
    (reset! globals/old-time new-time))
  (swap! current-frame-counter* #(rem (inc %) MAX-FRAMES-IN-FLIGHT)))

(defn destroy-semaphores-and-fences []
  (dotimes [i MAX-FRAMES-IN-FLIGHT]
    (let [frame (nth FRAMES i)]
      (VK13/vkDestroySemaphore (VulkanGlobals/getLogicalDevice) (get-image-available-semaphore-ptr frame) nil)
      (VK13/vkDestroySemaphore (VulkanGlobals/getLogicalDevice) (get-render-finished-semaphore-ptr frame) nil)
      (VK13/vkDestroyFence (VulkanGlobals/getLogicalDevice) ^Long (get-in-flight-fence-ptr frame) nil)))
  (alter-var-root #'FRAMES empty)
  (reset! current-frame-counter* 0))