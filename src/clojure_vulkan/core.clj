(set! *warn-on-reflection* true)

(ns clojure-vulkan.core
  (:require [clojure.edn :as edn]
            [clojure-vulkan.glfw :as glfw]
            [clojure-vulkan.globals :refer [*config* LOGICAL-DEVICE WINDOW-POINTER]]
            [clojure-vulkan.render :as render]
            [clojure-vulkan.util :as util]
            [clojure-vulkan.validation-layers :as validation-layers]
            [clojure-vulkan.vulkan :as vulkan]
            [clojure-vulkan.window :as window])
  (:import (org.lwjgl.vulkan VK13)
           (org.lwjgl.glfw GLFW)))

(require '[clojure.reflect :as reflect])

(defn -main [& _]
  (let [config (edn/read-string (slurp "config.edn"))]
    (binding [*config* config
              util/*current-debug-filename* (when (:file-debug config) (util/debug-filename))
              validation-layers/*enable-validation-layers* (:enable-validation-layers config)]
      (try
        ;; init
        (glfw/init)
        (window/create-window)
        (vulkan/init)
        (GLFW/glfwShowWindow WINDOW-POINTER)

        ;; application loop
        (while (not (window/should-window-close?))
          (glfw/poll-events)
          (render/draw-frame))
        (VK13/vkDeviceWaitIdle LOGICAL-DEVICE)

        (catch Throwable t
          (util/log "An error occured:" (.getMessage t)
                    (.printStackTrace t))
          #_(.printStackTrace t)
          (throw t))


        ;; termination
        (finally
          (util/try-all #(util/log "An error occurred in one of the major cleanup sections: " (.printStackTrace ^Throwable %))
            (window/destroy-window)
            (glfw/terminate)
            (vulkan/terminate)))))))