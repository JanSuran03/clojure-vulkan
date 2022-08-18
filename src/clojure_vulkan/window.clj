(ns clojure-vulkan.window
  (:require [clojure-vulkan.glfw :refer [glfw-boolean]]
            [clojure-vulkan.globals :as globals :refer [WINDOW-POINTER]]
            [clojure-vulkan.util :refer [nullptr]])
  (:import (org.lwjgl.glfw GLFW)))


(def ^Integer window-width 800)
(def ^Integer window-height 600)
(def ^String window-title "Hello Vulkan")

(defn -create-window []
  (if-let [window (GLFW/glfwCreateWindow window-width window-height window-title nullptr nullptr)]
    (globals/set-global! WINDOW-POINTER window)
    (throw (RuntimeException. "Failed to create GLFW window."))))

(defn create-window []
  (GLFW/glfwDefaultWindowHints)
  (GLFW/glfwWindowHint GLFW/GLFW_RESIZABLE (glfw-boolean false))
  (GLFW/glfwWindowHint GLFW/GLFW_CLIENT_API GLFW/GLFW_NO_API)
  (GLFW/glfwWindowHint GLFW/GLFW_VISIBLE (glfw-boolean false))
  (-create-window))

(defn destroy-window []
  (GLFW/glfwDestroyWindow WINDOW-POINTER)
  (globals/reset-window-ptr))

(defn should-window-close? [] (GLFW/glfwWindowShouldClose WINDOW-POINTER))
