(ns clojure-vulkan.debug
  (:require [clojure-vulkan.validation-layers :as validation-layers])
  (:import (org.lwjgl.vulkan VkDebugUtilsMessengerCallbackDataEXT)))

(def ^long debug-messenger)

(defn debug-callback [message-severity message-type callback-data-ptr user-data-ptr]
  (let [^VkDebugUtilsMessengerCallbackDataEXT callback-data (VkDebugUtilsMessengerCallbackDataEXT/create ^long callback-data-ptr)]
    (binding [*out* *err*]
      (println "Validation layer: " (.pMessageString callback-data)))))

(defn setup-debug-messenger []
  (when validation-layers/*enable-validation-layers*
    ;; TODO: setup debug messenger
    ))