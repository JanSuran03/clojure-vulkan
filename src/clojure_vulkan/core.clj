(ns clojure-vulkan.core
  (:require [clojure-vulkan.window :as window]
            [clojure-vulkan.vulkan :as vulkan]
            [clojure-vulkan.glfw :as glfw]))

(defn -main []
  (try
    ;; init
    (glfw/init)
    (vulkan/init)
    (window/create-window)
    (window/show-window)

    ;; application loop
    (while (not (window/should-window-close?))
      (glfw/poll-events))

    ;; termination
    (finally
      (window/destroy-window)
      (glfw/terminate)
      (vulkan/terminate))))