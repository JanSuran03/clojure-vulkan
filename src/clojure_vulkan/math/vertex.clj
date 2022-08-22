(ns clojure-vulkan.math.vertex
  (:require [clojure-vulkan.buffer :as buffer]
            [clojure-vulkan.globals :as globals :refer [INDEX-BUFFER-POINTER INDEX-BUFFER-MEMORY-POINTER LOGICAL-DEVICE
                                                        PHYSICAL-DEVICE VERTEX-BUFFER-MEMORY-POINTER VERTEX-BUFFER-POINTER]]
            [clojure-vulkan.math.glsl :as glsl]
            [clojure-vulkan.math.vector-2f]
            [clojure-vulkan.math.vector-3f]
            [clojure-vulkan.shaders :as shaders]
            [clojure-vulkan.util :as util])
  (:import (clojure_vulkan ShaderAnalyzer ShaderAnalyzer$ShaderLayout MemoryUtils)
           (clojure_vulkan.math.vector_2f Vector2f)
           (clojure_vulkan.math.vector_3f Vector3f)
           (org.lwjgl PointerBuffer)
           (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK13 VkBufferCreateInfo VkPhysicalDeviceMemoryProperties VkVertexInputAttributeDescription VkVertexInputBindingDescription)))

(deftype Vertex [^Vector2f pos ^Vector3f color])

(defn vertex [x y r g b]
  #_(Vertex. (Vector2f. x y)
             (Vector3f. r g b))
  (list x y r g b))

(def ^"[F" vertices (into-array Float/TYPE (concat (vertex -0.5 -0.5 1 0 0) ; red
                                                   (vertex +0.5 -0.5 0 1 0) ; green
                                                   (vertex +0.5 +0.5 0 0 1) ; blue
                                                   (vertex -0.5 +0.5 0.5 0 0.5) ; violet
                                                   )))

(def ^"[S" indices (into-array Short/TYPE [0 1 2 2 3 0]))

(defn analyze-shader-attribute-descriptions [shader-source]
  (map (memfn ^ShaderAnalyzer$ShaderLayout hashMap)
       (ShaderAnalyzer/analyze (slurp (str shaders/shader-sources-root shader-source)))))

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

(defn- find-memory-type [^Integer type-filter ^Integer memory-property-flags ^MemoryStack stack]
  (let [memory-properties (VkPhysicalDeviceMemoryProperties/malloc stack)]
    (VK13/vkGetPhysicalDeviceMemoryProperties PHYSICAL-DEVICE memory-properties)
    (or (some (fn [^Integer i]
                (when (and (not= 0 (bit-and type-filter (bit-shift-left 1 i)))
                           (= (bit-and (.propertyFlags (.memoryTypes memory-properties i))
                                       memory-property-flags)
                              memory-property-flags))
                  i))
              (range (.memoryTypeCount memory-properties)))
        (throw (RuntimeException. "Failed to find suitable memory type for the vertex buffer.")))))

(defn create-vertex-buffer []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [buffer-size (* ^int (:in-stride current-triangle-vbo-characterictics) ; bytes per vertex
                         (/ (count vertices)                ; vertices
                            (:components-per-vertex current-triangle-vbo-characterictics)))
          buffer-ptr* (.mallocLong stack 1)
          buffer-memory-ptr* (.mallocLong stack 1)

          [staging-buffer-ptr
           staging-buffer-memory-ptr
           ^VkBufferCreateInfo staging-buffer-create-info]
          (buffer/create-buffer buffer-size                 ;; create staging buffer
                                VK13/VK_BUFFER_USAGE_TRANSFER_SRC_BIT
                                (bit-or VK13/VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                                        VK13/VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT)
                                buffer-ptr*
                                buffer-memory-ptr*
                                stack)
          ^PointerBuffer data-ptr (.mallocPointer stack 1)
          _ (do (VK13/vkMapMemory LOGICAL-DEVICE staging-buffer-memory-ptr #_offset 0 buffer-size #_flags 0 data-ptr)
                (MemoryUtils/memcpyFloats (.getByteBuffer data-ptr 0 (int (.size staging-buffer-create-info)))
                                          vertices)
                (VK13/vkUnmapMemory LOGICAL-DEVICE staging-buffer-memory-ptr))
          [vertex-buffer-ptr
           vertex-buffer-memory-ptr]
          (buffer/create-buffer buffer-size                 ;; create vertex buffer
                                (bit-or VK13/VK_BUFFER_USAGE_VERTEX_BUFFER_BIT
                                        VK13/VK_BUFFER_USAGE_TRANSFER_DST_BIT)
                                (bit-or VK13/VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                                        VK13/VK_MEMORY_PROPERTY_HOST_COHERENT_BIT)
                                buffer-ptr*
                                buffer-memory-ptr*
                                stack)]
      (buffer/copy-buffer staging-buffer-ptr vertex-buffer-ptr buffer-size stack)
      (VK13/vkDestroyBuffer LOGICAL-DEVICE staging-buffer-ptr nil)
      (VK13/vkFreeMemory LOGICAL-DEVICE staging-buffer-memory-ptr nil)
      (do (globals/set-global! VERTEX-BUFFER-POINTER vertex-buffer-ptr)
          (globals/set-global! VERTEX-BUFFER-MEMORY-POINTER vertex-buffer-memory-ptr)))))

(defn create-index-buffer []
  (util/with-memory-stack-push ^MemoryStack stack
    (let [buffer-size (* (count indices) Short/SIZE)
          buffer-ptr* (.mallocLong stack 1)
          buffer-memory-ptr* (.mallocLong stack 1)
          [staging-buffer-ptr
           staging-buffer-memory-ptr]
          (buffer/create-buffer buffer-size
                                VK13/VK_BUFFER_USAGE_TRANSFER_SRC_BIT
                                (bit-or VK13/VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                                        VK13/VK_MEMORY_PROPERTY_HOST_COHERENT_BIT)
                                buffer-ptr*
                                buffer-memory-ptr*
                                stack)
          data (.mallocPointer stack 1)
          _ (VK13/vkMapMemory LOGICAL-DEVICE staging-buffer-memory-ptr #_offset 0 buffer-size #_flags 0 data)
          _ (MemoryUtils/memcpyShorts (.getByteBuffer data (int buffer-size)) indices)
          _ (VK13/vkUnmapMemory LOGICAL-DEVICE staging-buffer-memory-ptr)
          [index-buffer-ptr
           index-buffer-memory-ptr]
          (buffer/create-buffer buffer-size
                                (bit-or VK13/VK_BUFFER_USAGE_TRANSFER_DST_BIT
                                        VK13/VK_BUFFER_USAGE_INDEX_BUFFER_BIT)
                                VK13/VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT
                                buffer-ptr*
                                buffer-memory-ptr*
                                stack)]
      (buffer/copy-buffer staging-buffer-ptr index-buffer-ptr buffer-size stack)
      (VK13/vkDestroyBuffer LOGICAL-DEVICE staging-buffer-ptr nil)
      (VK13/vkFreeMemory LOGICAL-DEVICE staging-buffer-memory-ptr nil)
      (globals/set-global! INDEX-BUFFER-POINTER index-buffer-ptr)
      (globals/set-global! INDEX-BUFFER-MEMORY-POINTER index-buffer-memory-ptr))))

(defn destroy-vertex-buffer []
  (VK13/vkDestroyBuffer LOGICAL-DEVICE VERTEX-BUFFER-POINTER nil)
  (globals/reset-vertex-buffer-ptr))

(defn free-vertex-buffer-memory []
  (VK13/vkFreeMemory LOGICAL-DEVICE VERTEX-BUFFER-MEMORY-POINTER nil)
  (globals/reset-vertex-buffer-memory-ptr))

(defn destroy-index-buffer []
  (VK13/vkDestroyBuffer LOGICAL-DEVICE INDEX-BUFFER-POINTER nil)
  (globals/reset-index-buffer-ptr))

(defn free-index-buffer-memory []
  (VK13/vkFreeMemory LOGICAL-DEVICE INDEX-BUFFER-MEMORY-POINTER nil)
  (globals/reset-index-buffer-memory-ptr))