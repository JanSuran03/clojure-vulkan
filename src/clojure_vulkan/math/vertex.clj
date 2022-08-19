(ns clojure-vulkan.math.vertex
  (:require [clojure-vulkan.globals :as globals :refer [LOGICAL-DEVICE PHYSICAL-DEVICE VERTEX-BUFFER-MEMORY-POINTER VERTEX-BUFFER-POINTER]]
            [clojure-vulkan.math.glsl :as glsl]
            [clojure-vulkan.math.vector-2f]
            [clojure-vulkan.math.vector-3f]
            [clojure-vulkan.shaders :as shaders]
            [clojure-vulkan.util :as util])
  (:import (clojure_vulkan.math.vector_2f Vector2f)
           (clojure_vulkan.math.vector_3f Vector3f)
           (clojure_vulkan ShaderAnalyzer ShaderAnalyzer$ShaderLayout MemoryUtils)
           (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK13 VkBufferCreateInfo VkVertexInputAttributeDescription VkVertexInputBindingDescription VkMemoryRequirements VkPhysicalDeviceMemoryProperties VkMemoryAllocateInfo)
           (org.lwjgl PointerBuffer)))

(deftype Vertex [^Vector2f pos ^Vector3f color])

(defn vertex [x y r g b]
  #_(Vertex. (Vector2f. x y)
             (Vector3f. r g b))
  (list x y r g b))

(def ^"[F" vertices (into-array Float/TYPE (concat (vertex 0 -0.5 1.0 1.0 0)
                                                   (vertex 0.5 0.5 0 0.5 0.5)
                                                   (vertex -0.5 0.5 1 0 1))))

(defn analyze-shader-attribute-descriptions [shader-source]
  (map (memfn ^ShaderAnalyzer$ShaderLayout asHashMap)
       (ShaderAnalyzer/analyze (slurp (str shaders/shader-sources-root shader-source)))))

(defn analyze-shader-characteristics [shader-source]
  (let [attribute-descriptions (analyze-shader-attribute-descriptions shader-source)]
    (as-> attribute-descriptions m
          (map #(let [type (glsl/kw->type (:type %))]
                  (assoc % :component-sizeof (glsl/component-sizeof type)
                           :components (glsl/components type)
                           :format (glsl/format type)
                           :sizeof (glsl/sizeof type)))
               m)
          (group-by :mode m)
          (assoc m :in-stride (apply + (map :sizeof (:in m)))
                   :components-per-vertex (apply + (map :components (:in m)))))))

(def current-triangle-vbo-characterictics (analyze-shader-characteristics "shader.vert"))

(defn get-binding-descriptions [^MemoryStack stack]
  (let [input-binding-descriptions (VkVertexInputBindingDescription/calloc 1 stack)]
    (doto ^VkVertexInputBindingDescription (.get input-binding-descriptions 0)
      (.binding 0)                                          ;; binding id of the description
      (.stride ^int (:in-stride current-triangle-vbo-characterictics))
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
    (let [buffer-create-info (doto (VkBufferCreateInfo/calloc stack)
                               (.sType VK13/VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                               (.size (* ^int (:in-stride current-triangle-vbo-characterictics) ; bytes per vertex
                                         (/ (count vertices) ; vertices
                                            (:components-per-vertex current-triangle-vbo-characterictics))))
                               (.usage VK13/VK_BUFFER_USAGE_VERTEX_BUFFER_BIT)
                               (.sharingMode VK13/VK_SHARING_MODE_EXCLUSIVE)
                               (.flags 0))
          vertex-buffer-ptr (.mallocLong stack 1)
          _ (if (= (VK13/vkCreateBuffer LOGICAL-DEVICE buffer-create-info nil vertex-buffer-ptr)
                   VK13/VK_SUCCESS)
              (globals/set-global! VERTEX-BUFFER-POINTER (.get vertex-buffer-ptr 0))
              (throw (RuntimeException. "Failed to create vertex buffer.")))
          memory-requirements (VkMemoryRequirements/malloc stack)
          ;; alignment -> the offset in bytes where the buffer begins in the allocated region in memory (depends on usage and flags)
          _ (VK13/vkGetBufferMemoryRequirements LOGICAL-DEVICE VERTEX-BUFFER-POINTER memory-requirements)
          memory-allocate-info (doto (VkMemoryAllocateInfo/calloc stack)
                                 (.sType VK13/VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                                 (.allocationSize (.size memory-requirements)) ; in bytes, of course
                                 (.memoryTypeIndex (find-memory-type (.memoryTypeBits memory-requirements)
                                                                     (bit-or VK13/VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                                                                             VK13/VK_MEMORY_PROPERTY_HOST_COHERENT_BIT)
                                                                     stack)))
          vertex-buffer-memory-ptr (.mallocLong stack 1)
          _ (if (= (VK13/vkAllocateMemory LOGICAL-DEVICE memory-allocate-info nil vertex-buffer-memory-ptr)
                   VK13/VK_SUCCESS)
              (globals/set-global! VERTEX-BUFFER-MEMORY-POINTER (.get vertex-buffer-memory-ptr 0))
              (throw (RuntimeException. "Failed to allocate vertex buffer memory.")))
          _ (VK13/vkBindBufferMemory LOGICAL-DEVICE VERTEX-BUFFER-POINTER VERTEX-BUFFER-MEMORY-POINTER 0) ; 0 = memory offset
          ^PointerBuffer data-ptr (.mallocPointer stack 1)]
      (VK13/vkMapMemory LOGICAL-DEVICE VERTEX-BUFFER-MEMORY-POINTER #_offset 0 (.size buffer-create-info) #_flags 0 data-ptr)
      (MemoryUtils/memcpy (.getByteBuffer data-ptr 0 (int (.size buffer-create-info)))
                          vertices)
      (VK13/vkUnmapMemory LOGICAL-DEVICE VERTEX-BUFFER-MEMORY-POINTER))))

(defn destroy-vertex-buffer []
  (VK13/vkDestroyBuffer LOGICAL-DEVICE VERTEX-BUFFER-POINTER nil)
  (globals/reset-vertex-buffer-ptr))

(defn free-vertex-buffer-memory []
  (VK13/vkFreeMemory LOGICAL-DEVICE VERTEX-BUFFER-MEMORY-POINTER nil)
  (globals/reset-vertex-buffer-memory-ptr))