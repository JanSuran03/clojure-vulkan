(ns clojure-vulkan.util
  (:require [clojure.string :as str]
            [me.raynes.fs :as fs])
  (:import (java.io File)
           (java.util Date)
           (org.lwjgl.system MemoryStack MemoryUtil StructBuffer)
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

(def ^:dynamic *current-debug-filename* nil)

(def debug-prefix "debug__")

(defn debug-filename []
  (let [now (str/replace (Date.) #"\ " "_")]
    (str debug-prefix now ".txt")))

(defn delete-debug-files []
  (doseq [^File file (fs/list-dir "")
          :let [filename (-> file .getPath (str/split #"\/") last)]]
    (when (or (str/starts-with? filename debug-prefix)
              (str/starts-with? filename "hs_err_pid"))
      (fs/delete file))))

(defn string-seq-as-pointer-buffer [string-seq]
  (with-memory-stack-get ^MemoryStack stack
    (let [buffer (.mallocPointer stack (count string-seq))]
      (doseq [string string-seq]
        (.put buffer (.UTF8 stack string)))
      (.rewind buffer))))

(defn contains-keys [m & keys]
  (every? #(contains? m %) keys))

(comment
  (let [nss (->> (all-ns)
                 (filter #(str/starts-with? % "clojure-vulkan"))
                 (map (comp symbol str))
                 doall)]
    (dotimes [_ 5]
      (time (doseq [ns nss]
              (require `[~ns :reload true]))))))

(defn buffer->seq [^StructBuffer buffer]
  (-> buffer .iterator iterator-seq))

(defn clamp [^Integer min-value ^Integer value ^Integer max-value]
  (Math/min max-value (Math/max min-value value)))