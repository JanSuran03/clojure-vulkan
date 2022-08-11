(ns clojure-vulkan.shaders
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [me.raynes.fs :as fs])
  (:import (java.io ByteArrayOutputStream File)
           (org.lwjgl.util.shaderc Shaderc)
           (org.lwjgl.vulkan VK13)
           (java.nio ByteBuffer)
           (org.lwjgl.system NativeResource)))

(def shaders-root "resources/shaders/")
(def shader-sources-root                                    ;(.getAbsolutePath (File. (str shaders-root "sources/")))
  (str shaders-root "sources/"))
(def compiled-shaders-root                                  ;(.getAbsolutePath (File. (str shaders-root "compiled/")))
  (str shaders-root "compiled/"))

(defn- shader-type->integer-id [shader-type]
  (case shader-type
    :shader-type/vertex Shaderc/shaderc_glsl_vertex_shader
    :shader-type/fragment Shaderc/shaderc_glsl_fragment_shader
    :shader-type/geometry Shaderc/shaderc_glsl_geometry_shader
    (throw (RuntimeException. (str "Unknown shader type: " shader-type)))))

#_(defn- is-already-compiled? [shader-bytecode-out]
    (fs/exists? (str compiled-shaders-root shader-bytecode-out)))

#_(defn compile-shader
    "(compile-shader \"shader\" :fragment)
     => (compile-shader
     "
    ([shader-source-file] (compile-shader shader-source-file nil))
    ([shader-source-file shader-bytecode-out-file]
     (let [{:keys [err]} (if shader-bytecode-out-file
                           (sh/sh "glslc" (str "resources/shaders/sources/" shader-source-file)
                                  "-o"
                                  (str "resources/shaders/compiled/" shader-bytecode-out-file))
                           (sh/sh "glslc" (str "resources/shaders/sources/" shader-source-file)))]
       (if (pos? (.length ^CharSequence err))
         (throw (RuntimeException. (str "Couldn't compile shader (source =" shader-source-file
                                        "; out =" shader-bytecode-out-file ":\n" err)))))))

(defn slurp-bytes [source]
  (with-open [out (ByteArrayOutputStream.)]
    (io/copy (io/input-stream source) out)
    (let [signed-byte-out (.toByteArray out)
          ret (int-array (alength signed-byte-out))]
      (dotimes [i (alength signed-byte-out)]
        (aset-short ret i (Byte/toUnsignedInt (aget signed-byte-out i))))
      ret)))

#_(defn get-shader-in-spir-v-bytecode [shader-source shader-bytecode-out]
    (when-not (is-already-compiled? shader-bytecode-out) (compile-shader shader-source shader-bytecode-out))
    (slurp-bytes (str compiled-shaders-root shader-bytecode-out)))

;; let source = (seq (Files/readAllBytes (Path/of @#'shaders/shader-sources-root (into-array String ["shader.vert"]))))
;; or (slurp-bytes (str shader-sources-root \/ shader-source-file))
;; the tutorial way to do this, but why lol

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