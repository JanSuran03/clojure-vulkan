(ns clojure-vulkan.util
  (:refer-clojure :exclude [case])
  (:require [clojure.string :as str]
            [me.raynes.fs :as fs])
  (:import (java.io File)
           (java.util Date)
           (org.lwjgl.system MemoryStack MemoryUtil StructBuffer)
           (org.lwjgl.vulkan VK13)
           (org.joml Vector3f)))

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
  (let [err (volatile! nil)
        throw-expr (volatile! nil)
        try-eval (fn [condition]
                   (if-let [v (try (eval condition)
                                   (catch Throwable t (vreset! err t)))]
                     v
                     (if (nil? @err)
                       nil
                       (throw @err))))
        expanded (loop [[condition then :as clauses] clauses
                        ret []]
                   (cond (nil? condition)
                         ret

                         (nil? then)
                         (conj ret condition)

                         (= condition :case/default-throw)
                         (if (nil? @throw-expr)
                           (do (vreset! throw-expr then)
                               (recur (nnext clauses)
                                      ret))
                           (throw (IllegalStateException. (str "Cannot have two defaults:\n"
                                                               @throw-expr ";\n" then))))

                         :else
                         (recur (nnext clauses)
                                (conj ret (cond (symbol? condition)
                                                (try-eval condition)

                                                (list? condition)
                                                (apply list (map try-eval condition))

                                                :else
                                                condition)
                                      then))))]
    (if (even? (count expanded))
      `(clojure.core/case ~expr ~@expanded ~(deref throw-expr))
      `(clojure.core/case ~expr ~@expanded ~@(when-not (nil? @throw-expr) [@throw-expr])))))

(defn nths
  "Returns a seq of vectors of 1st item from each coll, then the second, etc.
  [[3 5 7] [6 10 14] [9 15 21] [12 20 28]] => ([3 6 9 12] [5 10 15 20] [7 14 21 28])"
  [& colls]
  (apply (partial map vector) colls))
