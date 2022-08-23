(ns clojure-vulkan.shaders
  (:refer-clojure :exclude [load])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure-vulkan.util :as util]
            [me.raynes.fs :as fs])
  (:import (java.io ByteArrayOutputStream File)
           (java.nio DirectByteBuffer Buffer)
           (org.lwjgl.system MemoryStack NativeResource)
           (org.lwjgl.util.shaderc Shaderc)
           (org.lwjgl.vulkan VK13)))

(defn direct-byte-buffer? [buf]
  (= (class buf) DirectByteBuffer))

(def shaders-root "resources/shaders/")
(def shader-sources-root (str shaders-root "sources/"))
(def compiled-shaders-root (str shaders-root "compiled/"))
(def ^String precompiled-shaders-database-file (str shaders-root "precompiled-vulkan-shaders.edn"))

(defn ^"[B" direct-byte-buffer->byte-array [^DirectByteBuffer byte-buffer]
  (let [arr (byte-array (.remaining byte-buffer))]
    (.get byte-buffer arr)
    arr))

(defprotocol IShaderDatabase
  (load [this])
  (add-src->bytecode-out [this src-file bytecode-file bytecode])
  (shader-modified? [this src-file])
  (reset-shaders-cache! [this]))

(deftype ShaderDatabase [^:unsynchronized-mutable shader-database]
  IShaderDatabase
  (load [this]
    (set! shader-database (try (edn/read-string (slurp precompiled-shaders-database-file))
                               (catch Throwable t
                                 (util/log "Couldn't open and edn-read precompiled shaders database file: "
                                           precompiled-shaders-database-file)
                                 (.printStackTrace t)
                                 (throw t)))))
  (add-src->bytecode-out [this src-file bytecode-file bytecode]
    (let [direct-byte-buffer? (direct-byte-buffer? bytecode)
          byte-array (if direct-byte-buffer?
                       (direct-byte-buffer->byte-array bytecode)
                       bytecode)
          new-database (assoc shader-database src-file {:last-modified (.lastModified (File. (str shader-sources-root src-file)))
                                                        :spv-filepath  bytecode-file})]
      (io/copy (io/input-stream byte-array)
               (File. ^String (str compiled-shaders-root bytecode-file)))
      (spit precompiled-shaders-database-file new-database)
      (set! shader-database new-database)
      (when direct-byte-buffer?
        (.rewind ^Buffer bytecode))))
  (shader-modified? [this src-file]
    (if-let [record-data (get shader-database src-file)]
      (or (nil? (:spv-filepath record-data))
          (not record-data)
          (> (.lastModified (File. (str shader-sources-root src-file)))
             (:last-modified record-data))
          (not (fs/exists? (str compiled-shaders-root (:spv-filepath record-data)))))
      true))
  (reset-shaders-cache! [this]
    (doseq [file (fs/list-dir compiled-shaders-root)]
      (fs/delete file))
    (set! shader-database {})
    (spit precompiled-shaders-database-file {})))

(def THE-DATABASE (ShaderDatabase. nil))

(when-not (fs/exists? precompiled-shaders-database-file)
  (util/log "Creating precompiled shders database file: " precompiled-shaders-database-file)
  (spit precompiled-shaders-database-file "{}"))

(load THE-DATABASE)

(defn- shader-type->integer-id [shader-type]
  (case shader-type
    :shader-type/vertex Shaderc/shaderc_glsl_vertex_shader
    :shader-type/fragment Shaderc/shaderc_glsl_fragment_shader
    :shader-type/geometry Shaderc/shaderc_glsl_geometry_shader
    (throw (RuntimeException. (str "Unknown shader type: " shader-type)))))

(defn ^"[B" slurp-bytes [source]
  (with-open [out (ByteArrayOutputStream.)]
    (io/copy (io/input-stream source) out)
    (.toByteArray out)))

(defprotocol ISpirVShader
  (get-bytebuffer [this]))

(deftype SpirVShader [address ^:unsynchronized-mutable bytecode]
  NativeResource
  (free [this]
    (when-not (= address :shader-state/already-compiled)
      (Shaderc/shaderc_result_release address))
    (set! bytecode nil))
  ISpirVShader
  (get-bytebuffer [this]
    (if (= DirectByteBuffer (class bytecode))
      bytecode
      (.bytes (MemoryStack/stackGet) ^"[B" bytecode))))

(defn ^SpirVShader compile-shader
  ([shader-source-file shader-type spv-filepath]
   (let [should-compile? (shader-modified? THE-DATABASE shader-source-file)]
     (if should-compile?
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
                                                           VK13/VK_NULL_HANDLE)
                  _ (do (when (= result VK13/VK_NULL_HANDLE)
                          (throw (RuntimeException. (str "Failed to compile shader " full-relative-path " into SPIR-V."))))
                        (when (not= (Shaderc/shaderc_result_get_compilation_status result)
                                    Shaderc/shaderc_compilation_status_success)
                          (throw (RuntimeException. (str "Failed to compile shader " full-relative-path " into SPIR-V:\n"
                                                         (Shaderc/shaderc_result_get_error_message result)))))
                        (Shaderc/shaderc_compiler_release compiler))
                  spv-bytecode (Shaderc/shaderc_result_get_bytes result)]
              (if (nil? spv-filepath)
                (util/log "Couldn't save precompiled shader bytecode: destination not supplied.")
                (add-src->bytecode-out THE-DATABASE shader-source-file spv-filepath spv-bytecode))
              (SpirVShader. result spv-bytecode))
            (catch Throwable t (.printStackTrace t)))
       (SpirVShader. :shader-state/already-compiled (.bytes (MemoryStack/stackGet)
                                                            (slurp-bytes (str compiled-shaders-root spv-filepath))))))))
