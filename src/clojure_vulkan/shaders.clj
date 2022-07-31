(ns clojure-vulkan.shaders
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [me.raynes.fs :as fs])
  (:import (java.io ByteArrayOutputStream)))

(def ^:private shaders-root "resources/shaders/")
(def ^:private shader-sources-root (str shaders-root "sources/"))
(def ^:private compiled-shaders-root (str shaders-root "compiled/"))

(defn- is-already-compiled? [shader-bytecode-out]
  (fs/exists? (str compiled-shaders-root shader-bytecode-out)))

(defn- compile-shader
  "(compile-shader \"shader\" :fragment)
   => (compile-shader
   "
  [shader-source shader-bytecode-out]
  (let [{:keys [err]} (sh/sh "glslc" (str "resources/shaders/sources/" shader-source)
                             "-o"
                             (str "resources/shaders/compiled/" shader-bytecode-out))]
    (if (pos? (.length ^CharSequence err))
      (throw (RuntimeException. (str "Couldn't compile shader (source =" shader-source
                                     "; out =" shader-bytecode-out ":\n" err))))))

(defn- slurp-bytes [source]
  (with-open [out (ByteArrayOutputStream.)]
    (io/copy (io/input-stream source) out)
    (let [signed-byte-out (.toByteArray out)
          ret (int-array (alength signed-byte-out))]
      (dotimes [i (alength signed-byte-out)]
        (aset-short ret i (Byte/toUnsignedInt (aget signed-byte-out i))))
      ret)))

(defn get-shader-in-spir-v-bytecode [shader-source shader-bytecode-out]
  (when-not (is-already-compiled? shader-bytecode-out) (compile-shader shader-source shader-bytecode-out))
  (slurp-bytes (str compiled-shaders-root shader-bytecode-out)))