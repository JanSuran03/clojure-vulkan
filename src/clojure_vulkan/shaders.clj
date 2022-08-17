(ns clojure-vulkan.shaders
  (:require [clojure.java.io :as io])
  (:import (java.io ByteArrayOutputStream)
           (java.nio ByteBuffer)
           (org.lwjgl.util.shaderc Shaderc)
           (org.lwjgl.system NativeResource)
           (org.lwjgl.vulkan VK13)))

(def shaders-root "resources/shaders/")
(def shader-sources-root (str shaders-root "sources/"))

(defn- shader-type->integer-id [shader-type]
  (case shader-type
    :shader-type/vertex Shaderc/shaderc_glsl_vertex_shader
    :shader-type/fragment Shaderc/shaderc_glsl_fragment_shader
    :shader-type/geometry Shaderc/shaderc_glsl_geometry_shader
    (throw (RuntimeException. (str "Unknown shader type: " shader-type)))))

(defn slurp-bytes [source]
  (with-open [out (ByteArrayOutputStream.)]
    (io/copy (io/input-stream source) out)
    (let [signed-byte-out (.toByteArray out)
          ret (int-array (alength signed-byte-out))]
      (dotimes [i (alength signed-byte-out)]
        (aset-short ret i (Byte/toUnsignedInt (aget signed-byte-out i))))
      ret)))

(defprotocol ISpirVShader
  (get-address [this])
  (get-bytebuffer [this]))

(deftype SpirVShader [address ^:unsynchronized-mutable ^ByteBuffer bytecode]
  NativeResource
  (free [this]
    (Shaderc/shaderc_result_release address)
    (set! bytecode nil))
  ISpirVShader
  (get-address [this] address)
  (get-bytebuffer [this] bytecode))

(defn compile-shader [shader-source-file shader-type]
  (try (let [full-relative-path (str shader-sources-root shader-source-file)
             compiler (Shaderc/shaderc_compiler_initialize)
             _ (when (= compiler VK13/VK_NULL_HANDLE)
                 (throw (RuntimeException. "Failed to initialize shader compiler.")))
             source (slurp full-relative-path)
             result (Shaderc/shaderc_compile_into_spv ^long compiler
                                                      ^String source
                                                      ^int (shader-type->integer-id shader-type)
                                                      full-relative-path
                                                      "main"
                                                      VK13/VK_NULL_HANDLE)]
         (when (= result VK13/VK_NULL_HANDLE)
           (throw (RuntimeException. (str "Failed to compile shader " full-relative-path " into SPIR-V."))))
         (when (not= (Shaderc/shaderc_result_get_compilation_status result)
                     Shaderc/shaderc_compilation_status_success)
           (throw (RuntimeException. (str "Failed to compile shader " full-relative-path " into SPIR-V:\n"
                                          (Shaderc/shaderc_result_get_error_message result)))))
         (Shaderc/shaderc_compiler_release compiler)
         (SpirVShader. result (Shaderc/shaderc_result_get_bytes result)))
       (catch Throwable t (.printStackTrace t))))