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

(defn partition-string-by [^String s ^long n]
  (let [len (.length s)]
    (loop [cur 0
           ret (transient [])]
      (if (>= cur len)
        (persistent! ret)
        (recur (+ cur n)
               (conj! ret (subs s cur (min len (+ cur n)))))))))

(defn split-string-on-lines-by [^String s n]
  (str/join \newline (partition-string-by s n)))

(def version-major 1)
(def version-minor 3)
(def vk-version (VK13/VK_MAKE_VERSION version-major version-minor 0))
(defmacro assert-not-null [v & body]
  `(if (nil? ~v)
     (throw (NullPointerException. ~(str "NullPointerException: " (resolve v))))
     (do ~@body)))

(defn bit-ors [& bits] (reduce bit-or bits))

(def ^:dynamic *doto-debug* false)

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

(defn string-seq-as-pointer-buffer [^MemoryStack stack string-seq]
  (let [buffer (.mallocPointer stack (count string-seq))]
    (doseq [string string-seq]
      (.put buffer (.UTF8 stack string)))
    (.rewind buffer)))

(defn integers-as-int-buffer [^MemoryStack stack integer-seq]
  (let [buffer (.mallocInt stack (count integer-seq))]
    (doseq [^int i integer-seq]
      (.put buffer i))
    (.rewind buffer)))

(defn app-nss [] (->> (all-ns)
                      (filter #(str/starts-with? % "clojure-vulkan"))
                      (map (comp symbol str))
                      doall))

(defn test-compile-speed []
  (let [nss (app-nss)]
    (println "Namespaces:" (count nss))
    (dotimes [_ 5]
      (time (doseq [ns nss]
              (require `[~ns :reload true]))))))

(defn struct-buffer->seq [^StructBuffer buffer]
  (-> buffer .iterator iterator-seq))

(defn clamp [^Integer min-value ^Integer value ^Integer max-value]
  (Math/min max-value (Math/max min-value value)))

(defmacro try-all [f & expressions]
  (let [gf (gensym "f__")]
    `(let [~gf ~f]
       (do ~@(map (fn [expr]
                    `(try ~expr
                          (catch Throwable t#
                            (~gf t#))))
                  expressions)))))