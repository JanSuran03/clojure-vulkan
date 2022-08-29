(ns clojure-vulkan.window
  (:require [clojure-vulkan.frame :as frame]
            [clojure-vulkan.glfw :refer [glfw-boolean]]
            [clojure-vulkan.globals :as globals :refer [WINDOW-POINTER]])
  (:import (clojure_vulkan GLFWKeyEvents)
    (org.lwjgl.glfw GLFW GLFWFramebufferSizeCallback GLFWKeyCallback)
    (org.lwjgl.vulkan VK13)))


(def ^Integer window-width 800)
(def ^Integer window-height 600)
(def ^String window-title "Hello Vulkan")

(defn -create-window []
  (if-let [window-ptr (GLFW/glfwCreateWindow window-width window-height window-title VK13/VK_NULL_HANDLE VK13/VK_NULL_HANDLE)]
    (globals/set-global! WINDOW-POINTER window-ptr)
    (throw (RuntimeException. "Failed to create GLFW window."))))

(defn create-window []
  (GLFW/glfwDefaultWindowHints)
  (GLFW/glfwWindowHint GLFW/GLFW_RESIZABLE (glfw-boolean true))
  (GLFW/glfwWindowHint GLFW/GLFW_CLIENT_API GLFW/GLFW_NO_API)
  (GLFW/glfwWindowHint GLFW/GLFW_VISIBLE (glfw-boolean false))
  (-create-window)
  (GLFW/glfwSetFramebufferSizeCallback WINDOW-POINTER (proxy [GLFWFramebufferSizeCallback] []
                                                        (invoke [window-ptr width height]
                                                          (reset! frame/FRAME-BUFFER-RESIZED? true))))
  (GLFW/glfwSetKeyCallback WINDOW-POINTER (proxy [GLFWKeyCallback] []
                                            (invoke [window-ptr key scancode action mods]
                                              (GLFWKeyEvents/processKeyEvent key scancode action mods)))))

(defn destroy-window []
  (GLFW/glfwDestroyWindow WINDOW-POINTER)
  (globals/reset-window-ptr))

(defn should-window-close? [] (GLFW/glfwWindowShouldClose WINDOW-POINTER))
