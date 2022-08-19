(ns clojure-vulkan.math.vertex
  (:require [clojure-vulkan.math.glsl :as glsl]
            [clojure-vulkan.math.vector-2f]
            [clojure-vulkan.math.vector-3f]
            [clojure-vulkan.shaders :as shaders])
  (:import (clojure_vulkan.math.vector_2f Vector2f)
           (clojure_vulkan.math.vector_3f Vector3f)
           (org.lwjgl.vulkan VkVertexInputBindingDescription VK13 VkVertexInputAttributeDescription)
           (org.lwjgl.system MemoryStack)
           (clojure_vulkan ShaderAnalyzer$ShaderLayout ShaderAnalyzer)))

(deftype Vertex [^Vector2f pos ^Vector3f color])

(defn vertex [x y r g b]
  (Vertex. (Vector2f. x y)
           (Vector3f. r g b)))

(def vertices [(vertex 0 -0.5 1 0 0)
               (vertex 0.5 0.5 0 1 0)
               (vertex -0.5 0.5 0 0 1)])

(defn analyze-type [type]
  (case type
    :vec2 {:bytes-per-component Float/BYTES :components 2}
    :vec3 {:bytes-per-component Float/BYTES :components 3}
    (throw (RuntimeException. (str "Invalid glsl type: " type)))))

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
          (assoc m :in-stride (apply + (map :sizeof (:in m)))))))

(defn get-binding-descriptions [{:keys [^int in-stride] :as _processed-shader-attributes}]
  (let [stack (MemoryStack/stackGet)
        input-binding-descriptions (VkVertexInputBindingDescription/calloc 1 stack)]
    (doto ^VkVertexInputBindingDescription (.get input-binding-descriptions 0)
      (.binding 0)                                          ;; binding id of the description
      (.stride in-stride)
      (.inputRate VK13/VK_VERTEX_INPUT_RATE_VERTEX))
    input-binding-descriptions))

(defn get-attribute-descriptions [{:keys [in] :as _processed-shader-attributes}]
  (let [stack (MemoryStack/stackGet)
        attribute-descriptions (VkVertexInputAttributeDescription/calloc ^int (count in) stack)]
    (reduce (fn [^Integer byte-offset {:keys [^int format ^int location ^int sizeof]}]
              (doto ^VkVertexInputAttributeDescription (.get attribute-descriptions location)
                (.binding 0)                                ;; id of the description this is bound to
                (.location location)
                (.format format))
              (+ byte-offset sizeof))
            0
            (sort-by :location < in))
    attribute-descriptions))