(ns clojure-vulkan.glfw
  (:import (org.lwjgl.glfw GLFW)))

(defmacro glfw-boolean [b] `(if ~b 1 0))

(defn init []
  (when-not (GLFW/glfwInit)
    (throw (RuntimeException. "Couldn't initialize GLFW."))))

(defn poll-events [] (GLFW/glfwPollEvents))
(defn terminate [] (GLFW/glfwTerminate))