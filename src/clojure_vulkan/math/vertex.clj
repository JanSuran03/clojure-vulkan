(ns clojure-vulkan.math.vertex
  (:require [clojure-vulkan.buffer :as buffer]
            [clojure-vulkan.globals :as globals :refer [INDEX-BUFFER VERTEX-BUFFER]]
            [clojure-vulkan.math.glsl :as glsl]
            [clojure-vulkan.math.vector-2f]
            [clojure-vulkan.math.vector-3f]
            [clojure-vulkan.shaders :as shaders]
            [clojure-vulkan.util :as util])
  (:import (clojure_vulkan ShaderAnalyzer)
           (clojure_vulkan.Vulkan Buffer VulkanGlobals)
           (clojure_vulkan.math.vector_2f Vector2f)
           (clojure_vulkan.math.vector_3f Vector3f)
           (org.lwjgl PointerBuffer)
           (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK13 VkBufferCreateInfo VkVertexInputAttributeDescription VkVertexInputBindingDescription)))

(deftype Vertex [^Vector2f pos ^Vector3f color])

(def ^"[F" vertices (into-array Float/TYPE
                                (concat (list -0.5 -0.5 -0.5 1 1 0)
                                        (list +0.5 -0.5 -0.5 1 0 1)
                                        (list +0.5 -0.5 +0.5 0 1 1)
                                        (list -0.5 -0.5 +0.5 1 0 0)
                                        (list -0.5 +0.5 -0.5 0 1 0)
                                        (list +0.5 +0.5 -0.5 0 0 1)
                                        (list +0.5 +0.5 +0.5 1 1 1)
                                        (list -0.5 +0.5 +0.5 0 0 0))))

(def ^"[S" indices (into-array Short/TYPE
                               [0 1 4, 1 5 4
                                1 2 5, 2 6 5
                                2 3 6, 3 7 6
                                3 0 7, 0 4 7
                                3 2 0, 2 1 0
                                4 5 7, 5 6 7]))

(defn analyze-shader-attribute-descriptions [shader-source]
  (ShaderAnalyzer/analyze (slurp (str shaders/shader-sources-root shader-source))))

(defn analyze-shader-characteristics [shader-source]
  (let [attribute-descriptions (analyze-shader-attribute-descriptions shader-source)]
    (as-> attribute-descriptions m
          (map #(if-let [type (glsl/kw->type (:type %))]
                  (assoc % :component-sizeof (glsl/component-sizeof type)
                           :components (glsl/components type)
                           :format (glsl/format type)
                           :sizeof (glsl/sizeof type))
                  %)
               m)
          (group-by :mode m)
          (assoc m :in-stride (apply + (map :sizeof (:in m)))
                   :components-per-vertex (apply + (map :components (:in m)))))))

(def current-triangle-vbo-characterictics (analyze-shader-characteristics "shader.vert"))

(defn get-binding-descriptions [^MemoryStack stack]
  (let [input-binding-descriptions (VkVertexInputBindingDescription/calloc 1 stack)]
    (doto ^VkVertexInputBindingDescription (.get input-binding-descriptions 0)
      (.binding 0)                                          ;; binding id of the description
      (.stride (int (:in-stride current-triangle-vbo-characterictics)))
      (.inputRate VK13/VK_VERTEX_INPUT_RATE_VERTEX))
    input-binding-descriptions))

(defn get-attribute-descriptions [^MemoryStack stack]
  (let [{:keys [^int in]} current-triangle-vbo-characterictics
        attribute-descriptions (VkVertexInputAttributeDescription/calloc ^int (count in) stack)]
    (reduce (fn [^Integer byte-offset {:keys [^int format ^int location ^int sizeof]}]
              (doto ^VkVertexInputAttributeDescription (.get attribute-descriptions location)
                (.binding 0)                                ;; id of the description this is bound to
                (.location location)
                (.format format)
                (.offset byte-offset))
              (+ byte-offset sizeof))
            0
            (sort-by :location < in))
    attribute-descriptions))

(defn create-vertex-buffer []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [buffer-size (* ^int (:in-stride current-triangle-vbo-characterictics) ; bytes per vertex
                         (/ (count vertices)                ; vertices
                            (:components-per-vertex current-triangle-vbo-characterictics)))
          buffer-ptr* (.mallocLong stack 1)
          buffer-memory-ptr* (.mallocLong stack 1)

          staging-buffer
          (buffer/create-buffer buffer-size                 ;; create staging buffer
                                VK13/VK_BUFFER_USAGE_TRANSFER_SRC_BIT
                                (bit-or VK13/VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                                        VK13/VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT)
                                buffer-ptr*
                                buffer-memory-ptr*
                                stack)
          ^PointerBuffer data-ptr* (.mallocPointer stack 1)
          _ (buffer/staging-buffer-memcpy (.bufferMemoryPointer staging-buffer) buffer-size data-ptr* vertices :buffer-copy/floats)
          vertex-buffer
          (buffer/create-buffer buffer-size                 ;; create vertex buffer
                                (bit-or VK13/VK_BUFFER_USAGE_VERTEX_BUFFER_BIT
                                        VK13/VK_BUFFER_USAGE_TRANSFER_DST_BIT)
                                (bit-or VK13/VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                                        VK13/VK_MEMORY_PROPERTY_HOST_COHERENT_BIT)
                                buffer-ptr*
                                buffer-memory-ptr*
                                stack)]
      (buffer/copy-buffer (.bufferPointer staging-buffer) (.bufferPointer vertex-buffer) buffer-size stack)
      (VK13/vkDestroyBuffer (VulkanGlobals/getLogicalDevice) (.bufferPointer staging-buffer) nil)
      (VK13/vkFreeMemory (VulkanGlobals/getLogicalDevice) (.bufferMemoryPointer staging-buffer) nil)
      (globals/set-global! VERTEX-BUFFER vertex-buffer))))

(defn create-index-buffer []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [buffer-size (* (count indices) Short/SIZE)
          buffer-ptr* (.mallocLong stack 1)
          buffer-memory-ptr* (.mallocLong stack 1)
          staging-buffer
          (buffer/create-buffer buffer-size
                                VK13/VK_BUFFER_USAGE_TRANSFER_SRC_BIT
                                (bit-or VK13/VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                                        VK13/VK_MEMORY_PROPERTY_HOST_COHERENT_BIT)
                                buffer-ptr*
                                buffer-memory-ptr*
                                stack)
          data-ptr* (.mallocPointer stack 1)
          _ (buffer/staging-buffer-memcpy (.bufferMemoryPointer staging-buffer) buffer-size data-ptr* indices :buffer-copy/shorts)
          index-buffer
          (buffer/create-buffer buffer-size
                                (bit-or VK13/VK_BUFFER_USAGE_TRANSFER_DST_BIT
                                        VK13/VK_BUFFER_USAGE_INDEX_BUFFER_BIT)
                                VK13/VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT
                                buffer-ptr*
                                buffer-memory-ptr*
                                stack)]
      (buffer/copy-buffer (.bufferPointer staging-buffer) (.bufferPointer index-buffer) buffer-size stack)
      (.free staging-buffer)
      (globals/set-global! INDEX-BUFFER index-buffer))))
