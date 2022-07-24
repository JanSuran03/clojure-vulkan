(ns clojure-vulkan.window
  (:require [clojure-vulkan.glfw :refer [glfw-boolean]]
            [clojure-vulkan.util :refer [nullptr]])
  (:import (org.lwjgl.glfw GLFW)))

(set! *warn-on-reflection* true)

(def ^Integer window-width 800)
(def ^Integer window-height 600)
(def ^String window-title "Hello Vulkan")
(def window-ptr nil)

(defn default-window-hints [] (GLFW/glfwDefaultWindowHints))
(defn hint-resizable [resizable?] (GLFW/glfwWindowHint GLFW/GLFW_RESIZABLE (glfw-boolean resizable?)))
(defn hint-no-client-api [] (GLFW/glfwWindowHint GLFW/GLFW_CLIENT_API GLFW/GLFW_NO_API))
(defn hint-visible [visible?] (GLFW/glfwWindowHint GLFW/GLFW_VISIBLE (glfw-boolean visible?)))
(defn show-window [] (println window-ptr) (GLFW/glfwShowWindow window-ptr))

(defn -create-window []
  (if-let [window (GLFW/glfwCreateWindow window-width window-height window-title nullptr nullptr)]
    (alter-var-root #'window-ptr
                    (constantly window))
    (throw (RuntimeException. "Failed to create GLFW window."))))

(defn create-window []
  (default-window-hints)
  (hint-resizable false)
  (hint-no-client-api)
  (hint-visible false)
  (-create-window))

(defn destroy-window []
  (GLFW/glfwDestroyWindow window-ptr)
  (alter-var-root #'window-ptr (constantly -1)))

(defn should-window-close? [] (GLFW/glfwWindowShouldClose window-ptr))
