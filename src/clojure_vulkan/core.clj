(ns clojure-vulkan.core
  (:require [clojure-vulkan.window :as window]
            [clojure-vulkan.vulkan :as vulkan]
            [clojure-vulkan.glfw :as glfw]))

(defn -main []
  (glfw/init)
  (window/create-window)
  (vulkan/init)
  (window/show-window)
  (while (not (window/should-window-close?))
    (glfw/poll-events))
  (window/destroy-window)
  (glfw/terminate))