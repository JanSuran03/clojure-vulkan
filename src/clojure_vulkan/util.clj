(ns clojure-vulkan.util
  (:refer-clojure :exclude [case])
  (:require [clojure.string :as str]
            [clojure-vulkan.globals :refer [PHYSICAL-DEVICE]]
            [me.raynes.fs :as fs])
  (:import (java.io File)
           (java.util Date)
           (org.lwjgl.system MemoryStack StructBuffer)
           (org.lwjgl.vulkan VK13 VkPhysicalDeviceMemoryProperties)))

(defmacro with-memory-stack-push [stack & body]
  `(with-open [^MemoryStack ~stack (MemoryStack/stackPush)]
     ~@body))

(defmacro log
  "Used to avoid `println` when searching for temporary logs (this function should
  be used for permanent logs)."
  [& args]
  `(println ~@args))

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

(defmacro bit-ors [& bits] (reduce bit-or (map eval bits)))

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

(defn app-nss []
  (->> (all-ns)
       (filter #(str/starts-with? % "clojure-vulkan"))
       (map (comp symbol str))
       doall))

(defn test-compile-speed []
  (let [nss (app-nss)]
    (log "Namespaces: " (count nss))
    (dotimes [_ 10]
      (time (doseq [ns nss]
              (require `[~ns :reload true]))))))

(defn compile-all []
  (let [nss (app-nss)]
    (time (doseq [ns nss]
            (time (compile ns))))))

(defn delete-compiled []
  (fs/delete-dir "target/classes/clojure_vulkan"))

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

(defmacro case
  "All symbols are evaluated, even in grouped case-lists. Can supply default via
  :case/default-throw to allow having a larger body at the bottom.

  (case result
    KHRSwapchain/VK_ERROR_OUT_OF_DATE_KHR
    :out-of-date

    :case/default-throw
    (throw (RuntimeException. \"Failed to acquire swap chain image.\"))

    (VK13/VK_SUCCESS KHRSwapchain/VK_SUBOPTIMAL_KHR)
    (do-something
      :really
      :really
      :long))

  (clojure.core/case result
    -1000001004
    :out-of-date

    (0 1000001003)
    (do-something
      :really
      :really
      :long)

    (throw (RuntimeException. \"Failed to acquire swap chain image.\")))"
  [expr & clauses]
  (let [try-eval (fn [condition]
                   (let [[v err] (try [(eval condition) nil]
                                      (catch Throwable t [nil t]))]
                     (if err
                       (binding [*out* *err*]
                         (log (str "ERROR: TRIED EVALUATING `" condition "` as an already defined constant"
                                   " expression. Did you forget to import the class?"))
                         (throw err))
                       v)))
        {:keys [expanded throw-expr*]}
        (loop [[condition then :as clauses] clauses
               ret []
               throw-expr* nil]
          (cond (nil? condition)
                {:expanded ret :throw-expr throw-expr*}

                (nil? then)
                {:expanded (conj ret condition) :throw-expr throw-expr*}

                (= condition :case/default-throw)
                (if (nil? throw-expr*)
                  (recur (nnext clauses)
                         ret
                         then)
                  (throw (IllegalStateException. (str "Cannot have two defaults:\n"
                                                      throw-expr* ";\n" then))))

                :else
                (recur (nnext clauses)
                       (conj ret (cond (symbol? condition) (try-eval condition)
                                       (list? condition) (apply list (map try-eval condition))
                                       :else condition)
                             then)
                       throw-expr*)))]
    (if (even? (count expanded))
      `(clojure.core/case ~expr ~@expanded ~throw-expr*)
      `(clojure.core/case ~expr ~@expanded ~@(when-not (nil? throw-expr*) [throw-expr*])))))

(defn nths
  "Returns a seq of vectors of 1st item from each coll, then the second, etc.
  [[a1 b1 c1] [a2 b2 c2] [a3 b3 c3] [a4 b4 c4]] => [a1 a2 a3 a4] [b1 b2 b3 b4] [c1 c2 c3 c4])"
  [& colls]
  (apply (partial map vector) colls))

(defn find-memory-type [^Integer type-filter ^Integer memory-property-flags ^MemoryStack stack kind]
  (let [memory-properties (VkPhysicalDeviceMemoryProperties/malloc stack)]
    (VK13/vkGetPhysicalDeviceMemoryProperties PHYSICAL-DEVICE memory-properties)
    (or (some (fn [^Integer i]
                (when (and (not= 0 (bit-and type-filter (bit-shift-left 1 i)))
                           (= (bit-and (.propertyFlags (.memoryTypes memory-properties i))
                                       memory-property-flags)
                              memory-property-flags))
                  i))
              (range (.memoryTypeCount memory-properties)))
        (throw (RuntimeException. "Failed to find suitable memory type for"
                                  (#{:memory-kind/vertex-buffer " the vertex buffer "
                                     :memory-kind/image " an image "}
                                   kind " <unknown> "))))))