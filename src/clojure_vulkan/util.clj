(ns clojure-vulkan.util
  (:import (org.lwjgl.system MemoryUtil MemoryStack)
           (org.lwjgl.glfw GLFW)
           (org.lwjgl.vulkan VK13)))

(defonce ^Long nullptr MemoryUtil/NULL)

(defmacro with-memory-stack-push [stack & body]
  `(with-open [^MemoryStack ~stack (MemoryStack/stackPush)]
     ~@body))

(defmacro with-memory-stack-get [stack & body]
  `(let [^MemoryStack ~stack (MemoryStack/stackGet)]
     ~@body))

(def version-major 1)
(def version-minor 3)
(def vk-version (VK13/VK_MAKE_VERSION version-major version-minor 0))
(defmacro assert-not-null [v & body]
  `(if (nil? ~v)
     (throw (NullPointerException. ~(str "NullPointerException: " (resolve v))))
     (do ~@body)))

(defn bit-ors [& bits] (reduce bit-or bits))

(def ^:dynamic *doto-debug* false)

(defn doto-debug [ret num]
  (when *doto-debug*
    (binding [*out* *err*]
      (println "Got there: " num))
    ret))